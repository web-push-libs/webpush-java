package nl.martijndwars.webpush;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPushService<T extends AbstractPushService<T>> {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final String SERVER_KEY_ID = "server-key-id";
    public static final String SERVER_KEY_CURVE = "P-256";

    /**
     * The Google Cloud Messaging API key (for pre-VAPID in Chrome)
     */
    private String gcmApiKey;

    /**
     * Subject used in the JWT payload (for VAPID). When left as null, then no subject will be used
     * (RFC-8292 2.1 says that it is optional)
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

    public AbstractPushService() {
    }

    public AbstractPushService(String gcmApiKey) {
        this.gcmApiKey = gcmApiKey;
    }

    public AbstractPushService(KeyPair keyPair) {
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
    }

    public AbstractPushService(KeyPair keyPair, String subject) {
        this(keyPair);
        this.subject = subject;
    }

    public AbstractPushService(String publicKey, String privateKey) throws GeneralSecurityException {
        this.publicKey = Utils.loadPublicKey(publicKey);
        this.privateKey = Utils.loadPrivateKey(privateKey);
    }

    public AbstractPushService(String publicKey, String privateKey, String subject) throws GeneralSecurityException {
        this(publicKey, privateKey);
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

    protected final HttpRequest prepareRequest(Notification notification, Encoding encoding) throws GeneralSecurityException, IOException, JoseException {
        if (getPrivateKey() != null && getPublicKey() != null) {
            if (!Utils.verifyKeyPair(getPrivateKey(), getPublicKey())) {
                throw new IllegalStateException("Public key and private key do not match.");
            }
        }

        Encrypted encrypted = encrypt(
                notification.getPayload(),
                notification.getUserPublicKey(),
                notification.getUserAuth(),
                encoding
        );

        byte[] dh = Utils.encode((ECPublicKey) encrypted.getPublicKey());
        byte[] salt = encrypted.getSalt();

        String url = notification.getEndpoint();
        Map<String, String> headers = new HashMap<>();
        byte[] body = null;

        headers.put("TTL", String.valueOf(notification.getTTL()));

        if (notification.hasUrgency()) {
            headers.put("Urgency", notification.getUrgency().getHeaderValue());
        }

        if (notification.hasTopic()) {
            headers.put("Topic", notification.getTopic());
        }


        if (notification.hasPayload()) {
            headers.put("Content-Type", "application/octet-stream");

            if (encoding == Encoding.AES128GCM) {
                headers.put("Content-Encoding", "aes128gcm");
            } else if (encoding == Encoding.AESGCM) {
                headers.put("Content-Encoding", "aesgcm");
                headers.put("Encryption", "salt=" + Base64.getUrlEncoder().withoutPadding().encodeToString(salt));
                headers.put("Crypto-Key", "dh=" + Base64.getUrlEncoder().encodeToString(dh));
            }

            body = encrypted.getCiphertext();
        }

        if (notification.isGcm()) {
            if (getGcmApiKey() == null) {
                throw new IllegalStateException("An GCM API key is needed to send a push notification to a GCM endpoint.");
            }

            headers.put("Authorization", "key=" + getGcmApiKey());
        } else if (vapidEnabled()) {
            if (encoding == Encoding.AES128GCM) {
                if (notification.getEndpoint().startsWith("https://fcm.googleapis.com")) {
                    url = notification.getEndpoint().replace("fcm/send", "wp");
                }
            }

            JwtClaims claims = new JwtClaims();
            claims.setAudience(notification.getOrigin());
            claims.setExpirationTimeMinutesInTheFuture(12 * 60);
            if (getSubject() != null) {
                claims.setSubject(getSubject());
            }

            JsonWebSignature jws = new JsonWebSignature();
            jws.setHeader("typ", "JWT");
            jws.setHeader("alg", "ES256");
            jws.setPayload(claims.toJson());
            jws.setKey(getPrivateKey());
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

            byte[] pk = Utils.encode((ECPublicKey) getPublicKey());

            if (encoding == Encoding.AES128GCM) {
                headers.put("Authorization", "vapid t=" + jws.getCompactSerialization() + ", k=" + Base64.getUrlEncoder().withoutPadding().encodeToString(pk));
            } else if (encoding == Encoding.AESGCM) {
                headers.put("Authorization", "WebPush " + jws.getCompactSerialization());
            }

            if (headers.containsKey("Crypto-Key")) {
                headers.put("Crypto-Key", headers.get("Crypto-Key") + ";p256ecdsa=" + Base64.getUrlEncoder().encodeToString(pk));
            } else {
                headers.put("Crypto-Key", "p256ecdsa=" + Base64.getUrlEncoder().encodeToString(pk));
            }
        } else if (notification.isFcm() && getGcmApiKey() != null) {
            headers.put("Authorization", "key=" + getGcmApiKey());
        }

        return new HttpRequest(url, headers, body);
    }

    /**
     * Set the Google Cloud Messaging (GCM) API key
     *
     * @param gcmApiKey
     * @return
     */
    public T setGcmApiKey(String gcmApiKey) {
        this.gcmApiKey = gcmApiKey;

        return (T) this;
    }

    public String getGcmApiKey() {
        return gcmApiKey;
    }

    public String getSubject() {
        return subject;
    }

    /**
     * Set the JWT subject (for VAPID)
     *
     * @param subject
     * @return
     */
    public T setSubject(String subject) {
        this.subject = subject;

        return (T) this;
    }

    /**
     * Set the public and private key (for VAPID).
     *
     * @param keyPair
     * @return
     */
    public T setKeyPair(KeyPair keyPair) {
        setPublicKey(keyPair.getPublic());
        setPrivateKey(keyPair.getPrivate());

        return (T) this;
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
    public T setPublicKey(String publicKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        setPublicKey(Utils.loadPublicKey(publicKey));

        return (T) this;
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
    public T setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;

        return (T) this;
    }

    /**
     * Set the public key using a base64url-encoded string.
     *
     * @param privateKey
     * @return
     */
    public T setPrivateKey(String privateKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        setPrivateKey(Utils.loadPrivateKey(privateKey));

        return (T) this;
    }

    /**
     * Set the private key (for VAPID)
     *
     * @param privateKey
     * @return
     */
    public T setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;

        return (T) this;
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
