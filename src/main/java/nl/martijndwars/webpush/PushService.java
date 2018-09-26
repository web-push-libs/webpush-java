package nl.martijndwars.webpush;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static nl.martijndwars.webpush.Utils.CURVE;

public class PushService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * The Google Cloud Messaging API key (for pre-VAPID in Chrome)
     */
    private String gcmApiKey;

    /**
     * Subject used in the JWT payload (for VAPID)
     */
    private String subject;

    /**
     * The public key (for VAPID)
     */
    private PublicKey publicKey;

    /**
     * The private key (for VAPID)
     */
    private PrivateKey privateKey;

    /**
     * Http client
     */
    private CloseableHttpAsyncClient closeableHttpAsyncClient;

    public PushService() {
    }

    public PushService(String gcmApiKey) {
        this.gcmApiKey = gcmApiKey;
    }

    public PushService(KeyPair keyPair, String subject) {
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        this.subject = subject;
    }

    public PushService(String publicKey, String privateKey, String subject) throws GeneralSecurityException {
        this.publicKey = Utils.loadPublicKey(publicKey);
        this.privateKey = Utils.loadPrivateKey(privateKey);
        this.subject = subject;
    }

    /**
     * Encrypt the getPayload using the user's public key using Elliptic Curve
     * Diffie Hellman cryptography over the prime256v1 curve.
     *
     * @return An Encrypted object containing the public key, salt, and
     * ciphertext, which can be sent to the other party.
     */
    public static Encrypted encrypt(byte[] buffer, PublicKey userPublicKey, byte[] userAuth, int padSize) throws GeneralSecurityException, IOException {
        ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(parameterSpec);

        KeyPair serverKey = keyPairGenerator.generateKeyPair();

        Map<String, KeyPair> keys = new HashMap<>();
        keys.put("server-key-id", serverKey);

        Map<String, String> labels = new HashMap<>();
        labels.put("server-key-id", "P-256");

        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);

        HttpEce httpEce = new HttpEce(keys, labels);
        byte[] ciphertext = httpEce.encrypt(buffer, salt, null, "server-key-id", userPublicKey, userAuth, padSize);

        return new Encrypted.Builder()
                .withSalt(salt)
                .withPublicKey(serverKey.getPublic())
                .withCiphertext(ciphertext)
                .build();
    }

    /**
     * Send a notification and wait for the response.
     *
     * @param notification
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws JoseException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public HttpResponse send(Notification notification) throws GeneralSecurityException, IOException, JoseException, ExecutionException, InterruptedException {
      try {
        return sendAsync(notification).get();
      } catch (ExecutionException e) {
        destroyClient();
        throw e;
      }
    }

  /**
     * Send a notification, but don't wait for the response.
     *
     * @param notification
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws JoseException
     */
    public Future<HttpResponse> sendAsync(Notification notification) throws GeneralSecurityException, IOException, JoseException {
        HttpPost httpPost = preparePost(notification);
        return getClient().execute(httpPost, new ClosableCallback(closeableHttpAsyncClient));
    }

  /**
     * Prepare a HttpPost for Apache async http client
     *
     * @param notification
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws JoseException
     */
    public HttpPost preparePost(Notification notification) throws GeneralSecurityException, IOException, JoseException {
        assert (verifyKeyPair());


        Encrypted encrypted = encrypt(
                notification.getPayload(),
                notification.getUserPublicKey(),
                notification.getUserAuth(),
                notification.getPadSize()
        );

        byte[] dh = Utils.savePublicKey((ECPublicKey) encrypted.getPublicKey());
        byte[] salt = encrypted.getSalt();

        HttpPost httpPost = new HttpPost(notification.getEndpoint());
        httpPost.addHeader("TTL", String.valueOf(notification.getTTL()));

        Map<String, String> headers = new HashMap<>();

        if (notification.hasPayload()) {
            headers.put("Content-Type", "application/octet-stream");
            headers.put("Content-Encoding", "aesgcm");
            headers.put("Encryption", "salt=" + Base64Encoder.encodeUrlWithoutPadding(salt));
            headers.put("Crypto-Key", "dh=" + Base64Encoder.encodeUrl(dh));

            httpPost.setEntity(new ByteArrayEntity(encrypted.getCiphertext()));
        }

        if (notification.isGcm()) {
            if (gcmApiKey == null) {
                throw new IllegalStateException("An GCM API key is needed to send a push notification to a GCM endpoint.");
            }

            headers.put("Authorization", "key=" + gcmApiKey);
        }

        if (vapidEnabled() && !notification.isGcm()) {
            JwtClaims claims = new JwtClaims();
            claims.setAudience(notification.getOrigin());
            claims.setExpirationTimeMinutesInTheFuture(12 * 60);
            claims.setSubject(subject);

            JsonWebSignature jws = new JsonWebSignature();
            jws.setHeader("typ", "JWT");
            jws.setHeader("alg", "ES256");
            jws.setPayload(claims.toJson());
            jws.setKey(privateKey);
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

            headers.put("Authorization", "WebPush " + jws.getCompactSerialization());

            byte[] pk = Utils.savePublicKey((ECPublicKey) publicKey);

            if (headers.containsKey("Crypto-Key")) {
                headers.put("Crypto-Key", headers.get("Crypto-Key") + ";p256ecdsa=" + Base64Encoder.encodeUrlWithoutPadding(pk));
            } else {
                headers.put("Crypto-Key", "p256ecdsa=" + Base64Encoder.encodeUrl(pk));
            }
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
        }
        return httpPost;
    }

    private boolean verifyKeyPair() {
        ECNamedCurveParameterSpec curveParameters = ECNamedCurveTable.getParameterSpec(CURVE);
        ECPoint g = curveParameters.getG();
        ECPoint sG = g.multiply(((ECPrivateKey) privateKey).getS());

        return sG.equals(((ECPublicKey) publicKey).getQ());
    }

    /**
     * Set the Google Cloud Messaging (GCM) API key
     *
     * @param gcmApiKey
     * @return
     */
    public PushService setGcmApiKey(String gcmApiKey) {
        this.gcmApiKey = gcmApiKey;

        return this;
    }

    /**
     * Set the JWT subject (for VAPID)
     *
     * @param subject
     * @return
     */
    public PushService setSubject(String subject) {
        this.subject = subject;

        return this;
    }

    /**
     * Set the public and private key (for VAPID).
     *
     * @param keyPair
     * @return
     */
    public PushService setKeyPair(KeyPair keyPair) {
        setPublicKey(keyPair.getPublic());
        setPrivateKey(keyPair.getPrivate());

        return this;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Set the public key using a base64url-encoded string.
     *
     * @param publicKey
     * @return
     */
    public PushService setPublicKey(String publicKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        setPublicKey(Utils.loadPublicKey(publicKey));

        return this;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public KeyPair getKeyPair() {
        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Set the public key (for VAPID)
     *
     * @param publicKey
     * @return
     */
    public PushService setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;

        return this;
    }

    /**
     * Set the public key using a base64url-encoded string.
     *
     * @param privateKey
     * @return
     */
    public PushService setPrivateKey(String privateKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        setPrivateKey(Utils.loadPrivateKey(privateKey));

        return this;
    }

    /**
     * Set the private key (for VAPID)
     *
     * @param privateKey
     * @return
     */
    public PushService setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;

        return this;
    }

    /**
     * Check if VAPID is enabled
     *
     * @return
     */
    protected boolean vapidEnabled() {
        return publicKey != null && privateKey != null;
    }

    private CloseableHttpAsyncClient getClient() {
      if(closeableHttpAsyncClient == null) {
        closeableHttpAsyncClient = HttpAsyncClients.createSystem();
        closeableHttpAsyncClient.start();
      }
      return closeableHttpAsyncClient;
    }

    private void destroyClient() throws IOException {
      try {
        getClient().close();
      } finally {
        closeableHttpAsyncClient = null;
      }
    }
}
