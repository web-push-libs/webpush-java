package nl.martijndwars.webpush;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Duration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationTest {

    private static final String endpoint = "https://the-url.co.uk";
    private static final String publicKey = "BGu3hOwCLOBfdMReXf7-SD2x5tKs_vPapOneyngBOnu6PgNYdgLPKFAodfBnG60MqkXC0McPFehN2Kyuh6TKm14=";
    private static int oneDayDurationInSeconds = 86400;

    @BeforeAll
    public static void addSecurityProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testNotificationBuilder() throws GeneralSecurityException {
        Notification notification = Notification.builder()
                .endpoint(endpoint)
                .userPublicKey(publicKey)
                .payload(new byte[16])
                .ttl((int) Duration.ofDays(15).getSeconds())
                .build();
        assertEquals(endpoint, notification.getEndpoint());
        assertEquals(15 * oneDayDurationInSeconds, notification.getTTL());
    }

    @Test
    public void testDefaultTtl() throws GeneralSecurityException {
        Notification notification = Notification.builder()
                .userPublicKey(publicKey)
                .payload(new byte[16])
                .build();
        assertEquals(28 * oneDayDurationInSeconds, notification.getTTL());
    }
}
