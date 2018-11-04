package nl.martijndwars.webpush;

import org.bouncycastle.jce.interfaces.ECPublicKey;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Notification {
    /**
     * The endpoint associated with the push subscription
     */
    private final String endpoint;

    /**
     * The client's public key
     */
    private final ECPublicKey userPublicKey;

    /**
     * The client's auth
     */
    private final byte[] userAuth;

    /**
     * An arbitrary payload
     */
    private final byte[] payload;

    /**
     * Time in seconds that the push message is retained by the push service
     */
    private final int ttl;


    public Notification(String endpoint, ECPublicKey userPublicKey, byte[] userAuth, byte[] payload, int ttl) {
        this.endpoint = endpoint;
        this.userPublicKey = userPublicKey;
        this.userAuth = userAuth;
        this.payload = payload;
        this.ttl = ttl;
    }

    public Notification(String endpoint, PublicKey userPublicKey, byte[] userAuth, byte[] payload, int ttl) {
        this(endpoint, (ECPublicKey) userPublicKey, userAuth, payload, ttl);
    }

    public Notification(String endpoint, PublicKey userPublicKey, byte[] userAuth, byte[] payload) {
        this(endpoint, userPublicKey, userAuth, payload, 2419200);
    }

    public Notification(String endpoint, String userPublicKey, String userAuth, byte[] payload) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this(endpoint, Utils.loadPublicKey(userPublicKey), Base64Encoder.decode(userAuth), payload);
    }

    public Notification(String endpoint, String userPublicKey, String userAuth, String payload) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this(endpoint, Utils.loadPublicKey(userPublicKey), Base64Encoder.decode(userAuth), payload.getBytes(UTF_8));
    }

    public Notification(Subscription subscription, String payload) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this(subscription.endpoint, subscription.keys.p256dh, subscription.keys.auth, payload);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public ECPublicKey getUserPublicKey() {
        return userPublicKey;
    }

    public byte[] getUserAuth() {
        return userAuth;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean hasPayload() {
        return getPayload().length > 0;
    }

    /**
     * Detect if the notification is for a GCM-based subscription
     *
     * @return
     */
    public boolean isGcm() {
        return getEndpoint().indexOf("https://android.googleapis.com/gcm/send") == 0;
    }

    public int getTTL() {
        return ttl;
    }

    public String getOrigin() throws MalformedURLException {
        URL url = new URL(getEndpoint());

        return url.getProtocol() + "://" + url.getHost();
    }
}
