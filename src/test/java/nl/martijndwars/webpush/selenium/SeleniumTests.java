package nl.martijndwars.webpush.selenium;

import nl.martijndwars.webpush.Base64Encoder;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * SeleniumTest performs integration testing.
 */
public class SeleniumTests {
    protected static String PUBLIC_KEY = "BNFDO1MUnNpx0SuQyQcAAWYETa2+W8z/uc5sxByf/UZLHwAhFLwEDxS5iB654KHiryq0AxDhFXS7DVqXDKjjN+8=";
    protected static String PRIVATE_KEY = "AM0aAyoIryzARADnIsSCwg1p1aWFAL3Idc8dNXpf74MH";

    protected static TestingService testingService = new TestingService("http://localhost:8090/api/");
    protected static int testSuiteId;

    protected PushService pushService;

    public SeleniumTests() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());

        pushService = new PushService(PUBLIC_KEY, PRIVATE_KEY, "http://localhost:8090");
    }

    /**
     * End the test suite.
     *
     * @throws IOException
     */
    @AfterAll
    public static void tearDown() throws IOException {
        testingService.endTestSuite(testSuiteId);
    }

    /**
     * Generate a stream of tests based on the configurations.
     *
     * @return
     */
    @TestFactory
    public Stream<DynamicTest> dynamicTests() throws IOException {
        testSuiteId = testingService.startTestSuite();

        return getConfigurations().map(configuration -> {
            BrowserTest browserTest = new BrowserTest(pushService, testingService, configuration, testSuiteId);

            return dynamicTest(browserTest.getDisplayName(), browserTest);
        });
    }

    /**
     * Get browser configurations to test.
     *
     * @return
     */
    protected Stream<Configuration> getConfigurations() {
        String PUBLIC_KEY_NO_PADDING = Base64Encoder.encodeWithoutPadding(
                Base64Encoder.decode(PUBLIC_KEY)
        );

        return Stream.of(
                new Configuration("chrome", "stable", PUBLIC_KEY_NO_PADDING),
                new Configuration("chrome", "beta", PUBLIC_KEY_NO_PADDING),
                new Configuration("chrome", "unstable", PUBLIC_KEY_NO_PADDING),

                new Configuration("firefox", "stable", PUBLIC_KEY_NO_PADDING),
                new Configuration("firefox", "beta", PUBLIC_KEY_NO_PADDING)
        );
    }
}
