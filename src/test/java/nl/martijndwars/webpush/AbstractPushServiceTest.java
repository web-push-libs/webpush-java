package nl.martijndwars.webpush;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractPushServiceTest {

    private static final String USER_PUBLIC_KEY = "BGu3hOwCLOBfdMReXf7-SD2x5tKs_vPapOneyngBOnu6PgNYdgLPKFAodfBnG60MqkXC0McPFehN2Kyuh6TKm14=";

    @BeforeAll
    public static void addSecurityProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void vapidCryptoKeyUsesUnpaddedBase64Url() throws Exception {
        ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(parameterSpec);

        TestPushService service = new TestPushService(keyPairGenerator.generateKeyPair());
        String userAuth = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
        Notification notification = new Notification(
                "https://push.example.test/send/123",
                USER_PUBLIC_KEY,
                userAuth,
                "payload"
        );

        HttpRequest request = service.prepare(notification, Encoding.AES128GCM);
        String cryptoKey = request.getHeaders().get("Crypto-Key");

        assertNotNull(cryptoKey);
        assertTrue(cryptoKey.startsWith("p256ecdsa="));
        assertFalse(cryptoKey.substring("p256ecdsa=".length()).contains("="));
    }

    private static class TestPushService extends AbstractPushService<TestPushService> {
        TestPushService(KeyPair keyPair) {
            super(keyPair);
        }

        HttpRequest prepare(Notification notification, Encoding encoding) throws Exception {
            return prepareRequest(notification, encoding);
        }
    }
}
