package nl.martijndwars.webpush;

import org.bouncycastle.jce.interfaces.ECPublicKey;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

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
     * Push Message Urgency
     *
     *  @see <a href="https://tools.ietf.org/html/rfc8030#section-5.3">Push Message Urgency</a>
     *
     */
    private Urgency urgency;

    /**
     * Push Message Topic
     *
     *  @see <a href="https://tools.ietf.org/html/rfc8030#section-5.4">Replacing Push Messages</a>
     *
     */
    private String topic;

    /**
     * Time in seconds that the push message is retained by the push service
     */
    private final int ttl;

    private static final int ONE_DAY_DURATION_IN_SECONDS = 86400;
    private static int DEFAULT_TTL = 28 * ONE_DAY_DURATION_IN_SECONDS;

    public Notification(String endpoint, ECPublicKey userPublicKey, byte[] userAuth, byte[] payload, int ttl, Urgency urgency, String topic) {
        this.endpoint = endpoint;
        this.userPublicKey = userPublicKey;
        this.userAuth = userAuth;
        this.payload = payload;
        this.ttl = ttl;
        this.urgency = urgency;
        this.topic = topic;
    }

    public Notification(String endpoint, PublicKey userPublicKey, byte[] userAuth, byte[] payload, int ttl) {
        this(endpoint, (ECPublicKey) userPublicKey, userAuth, payload, ttl, null, null);
    }

    public Notification(String endpoint, String userPublicKey, String userAuth, byte[] payload, int ttl)  throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this(endpoint, Utils.loadPublicKey(userPublicKey), Base64.getUrlDecoder().decode(userAuth), payload, ttl);
    }

    public Notification(String endpoint, PublicKey userPublicKey, byte[] userAuth, byte[] payload) {
        this(endpoint, userPublicKey, userAuth, payload, DEFAULT_TTL);
    }

    public Notification(String endpoint, String userPublicKey, String userAuth, byte[] payload) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this(endpoint, Utils.loadPublicKey(userPublicKey), Base64.getUrlDecoder().decode(userAuth), payload);
    }

    public Notification(String endpoint, String userPublicKey, String userAuth, String payload) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this(endpoint, Utils.loadPublicKey(userPublicKey), Base64.getUrlDecoder().decode(userAuth), payload.getBytes(UTF_8));
    }

	public Notification(String endpoint, String userPublicKey, String userAuth, String payload, Urgency urgency) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		this(endpoint, Utils.loadPublicKey(userPublicKey), Base64.getUrlDecoder().decode(userAuth), payload.getBytes(UTF_8));
		this.urgency = urgency;
	}

    public Notification(Subscription subscription, String payload) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this(subscription.endpoint, subscription.keys.p256dh, subscription.keys.auth, payload);
    }

    public Notification(Subscription subscription, String payload, Urgency urgency) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        this(subscription.endpoint, subscription.keys.p256dh, subscription.keys.auth, payload);
        this.urgency = urgency;
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

    public boolean hasUrgency() {
        return urgency != null;
    }

    public boolean hasTopic() {
        return topic != null;
    }

    /**
     * Detect if the notification is for a GCM-based subscription
     *
     * @return
     */
    public boolean isGcm() {
        return getEndpoint().indexOf("https://android.googleapis.com/gcm/send") == 0;
    }

    public boolean isFcm() {
        return getEndpoint().indexOf("https://fcm.googleapis.com/fcm/send") == 0;
    }

    public int getTTL() {
        return ttl;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public String getTopic() {
        return topic;
    }

    public String getOrigin() throws MalformedURLException {
        URL url = new URL(getEndpoint());

        return url.getProtocol() + "://" + url.getHost();
    }

    public static NotificationBuilder builder() {
        return new Notification.NotificationBuilder();
    }

    public static class NotificationBuilder {
        private String endpoint = null;
        private ECPublicKey userPublicKey = null;
        private byte[] userAuth = null;
        private byte[] payload = null;
        private int ttl = DEFAULT_TTL;
        private Urgency urgency = null;
        private String topic = null;

        private NotificationBuilder() {
        }

        public Notification build() {
            return new Notification(endpoint, userPublicKey, userAuth, payload, ttl, urgency, topic);
        }

        public NotificationBuilder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public NotificationBuilder userPublicKey(PublicKey publicKey) {
            this.userPublicKey = (ECPublicKey) publicKey;
            return this;
        }

        public NotificationBuilder userPublicKey(String publicKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
            this.userPublicKey = (ECPublicKey) Utils.loadPublicKey(publicKey);
            return this;
        }

        public NotificationBuilder userPublicKey(byte[] publicKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
            this.userPublicKey = (ECPublicKey) Utils.loadPublicKey(publicKey);
            return this;
        }

        public NotificationBuilder userAuth(String userAuth) {
            this.userAuth = Base64.getUrlDecoder().decode(userAuth);
            return this;
        }

        public NotificationBuilder userAuth(byte[] userAuth) {
            this.userAuth = userAuth;
            return this;
        }

        public NotificationBuilder payload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public NotificationBuilder payload(String payload) {
            this.payload = payload.getBytes(UTF_8);
            return this;
        }

        public NotificationBuilder ttl(int ttl) {
            this.ttl = ttl;
            return this;
        }

        public NotificationBuilder urgency(Urgency urgency) {
            this.urgency = urgency;
            return this;
        }

        public NotificationBuilder topic(String topic) {
            this.topic = topic;
            return this;
        }
    }

}
