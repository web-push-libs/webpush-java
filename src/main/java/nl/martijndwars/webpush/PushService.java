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
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.net.URI;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PushService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final String SERVER_KEY_ID = "server-key-id";
    public static final String SERVER_KEY_CURVE = "P-256";

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
     * Encrypt the payload.
     *
     * Encryption uses Elliptic curve Diffie-Hellman (ECDH) cryptography over the prime256v1 curve.
     *
     * @param payload       Payload to encrypt.
     * @param userPublicKey The user agent's public key (keys.p256dh).
     * @param userAuth      The user agent's authentication secret (keys.auth).
     * @param encoding
     * @return An Encrypted object containing the public key, salt, and ciphertext.
     * @throws GeneralSecurityException
     */
    public static Encrypted encrypt(byte[] payload, ECPublicKey userPublicKey, byte[] userAuth, Encoding encoding) throws GeneralSecurityException {
        KeyPair localKeyPair = generateLocalKeyPair();

        Map<String, KeyPair> keys = new HashMap<>();
        keys.put(SERVER_KEY_ID, localKeyPair);

        Map<String, String> labels = new HashMap<>();
        labels.put(SERVER_KEY_ID, SERVER_KEY_CURVE);

        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);

        HttpEce httpEce = new HttpEce(keys, labels);
        byte[] ciphertext = httpEce.encrypt(payload, salt, null, SERVER_KEY_ID, userPublicKey, userAuth, encoding);

        return new Encrypted.Builder()
                .withSalt(salt)
                .withPublicKey(localKeyPair.getPublic())
                .withCiphertext(ciphertext)
                .build();
    }

    /**
     * Generate the local (ephemeral) keys.
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     */
    private static KeyPair generateLocalKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(parameterSpec);

        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Send a notification and wait for the response.
     *
     * @param notification
     * @param encoding
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws JoseException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public HttpResponse send(Notification notification, Encoding encoding) throws GeneralSecurityException, IOException, JoseException, ExecutionException, InterruptedException {
        return sendAsync(notification, encoding).get();
    }

    public HttpResponse send(Notification notification) throws GeneralSecurityException, IOException, JoseException, ExecutionException, InterruptedException {
        return send(notification, Encoding.AESGCM);
    }

    /**
     * Send a notification, but don't wait for the response.
     *
     * @param notification
     * @param encoding
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws JoseException
     */
    public Future<HttpResponse> sendAsync(Notification notification, Encoding encoding) throws GeneralSecurityException, IOException, JoseException {
        HttpPost httpPost = preparePost(notification, encoding);

        final CloseableHttpAsyncClient closeableHttpAsyncClient = HttpAsyncClients.createSystem();
        closeableHttpAsyncClient.start();

        return closeableHttpAsyncClient.execute(httpPost, new ClosableCallback(closeableHttpAsyncClient));
    }

    public Future<HttpResponse> sendAsync(Notification notification) throws GeneralSecurityException, IOException, JoseException {
        return sendAsync(notification, Encoding.AESGCM);
    }

    /**
     * Prepare a HttpPost for Apache async http client
     *
     * @param notification
     * @param encoding
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws JoseException
     */
    public HttpPost preparePost(Notification notification, Encoding encoding) throws GeneralSecurityException, IOException, JoseException {
        assert (Utils.verifyKeyPair(privateKey, publicKey));

        Encrypted encrypted = encrypt(
                notification.getPayload(),
                notification.getUserPublicKey(),
                notification.getUserAuth(),
                encoding
        );

        byte[] dh = Utils.encode((ECPublicKey) encrypted.getPublicKey());
        byte[] salt = encrypted.getSalt();

        HttpPost httpPost = new HttpPost(notification.getEndpoint());
        httpPost.addHeader("TTL", String.valueOf(notification.getTTL()));

        Map<String, String> headers = new HashMap<>();

        if (notification.hasPayload()) {
            headers.put("Content-Type", "application/octet-stream");

            if (encoding == Encoding.AES128GCM) {
                headers.put("Content-Encoding", "aes128gcm");
            } else if (encoding == Encoding.AESGCM) {
                headers.put("Content-Encoding", "aesgcm");
                headers.put("Encryption", "salt=" + Base64Encoder.encodeUrlWithoutPadding(salt));
                headers.put("Crypto-Key", "dh=" + Base64Encoder.encodeUrl(dh));
            }

            httpPost.setEntity(new ByteArrayEntity(encrypted.getCiphertext()));
        }

        if (notification.isGcm()) {
            if (gcmApiKey == null) {
                throw new IllegalStateException("An GCM API key is needed to send a push notification to a GCM endpoint.");
            }

            headers.put("Authorization", "key=" + gcmApiKey);
        } else if (vapidEnabled()) {
            if (encoding == Encoding.AES128GCM) {
                if (notification.getEndpoint().startsWith("https://fcm.googleapis.com")) {
                    httpPost.setURI(URI.create(notification.getEndpoint().replace("fcm/send", "wp")));
                }
            }

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

            byte[] pk = Utils.encode((ECPublicKey) publicKey);

            if (encoding == Encoding.AES128GCM) {
                headers.put("Authorization", "vapid t=" + jws.getCompactSerialization() + ", k=" + Base64Encoder.encodeUrlWithoutPadding(pk));
            } else if (encoding == Encoding.AESGCM) {
                headers.put("Authorization", "WebPush " + jws.getCompactSerialization());
            }

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
}
