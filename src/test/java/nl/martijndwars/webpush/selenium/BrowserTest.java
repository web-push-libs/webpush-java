package nl.martijndwars.webpush.selenium;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.function.Executable;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BrowserTest implements Executable {
    public static final String GCM_API_KEY = "AIzaSyBAU0VfXoskxUSg81K5VgLgwblHbZWe6tA";
    public static final String PUBLIC_KEY = "BNFDO1MUnNpx0SuQyQcAAWYETa2+W8z/uc5sxByf/UZLHwAhFLwEDxS5iB654KHiryq0AxDhFXS7DVqXDKjjN+8=";
    public static final String PRIVATE_KEY = "AM0aAyoIryzARADnIsSCwg1p1aWFAL3Idc8dNXpf74MH";
    public static final String VAPID_SUBJECT = "http://localhost:8090";

    private TestingService testingService;
    private Configuration configuration;
    private int testSuiteId;

    public BrowserTest(TestingService testingService, Configuration configuration, int testSuiteId) {
        this.configuration = configuration;
        this.testingService = testingService;
        this.testSuiteId = testSuiteId;
    }

    /**
     * Execute the test for the given browser configuration.
     *
     * @throws Throwable
     */
    @Override
    public void execute() throws Throwable {
        PushService pushService = getPushService();

        JsonObject test = testingService.getSubscription(testSuiteId, configuration);

        int testId = test.get("testId").getAsInt();

        Subscription subscription = new Gson().fromJson(test.get("subscription").getAsJsonObject(), Subscription.class);

        String message = "Hëllö, world!";
        Notification notification = new Notification(subscription, message);

        HttpResponse response = pushService.send(notification);
        assertEquals(201, response.getStatusLine().getStatusCode());

        JsonArray messages = testingService.getNotificationStatus(testSuiteId, testId);
        assertEquals(1, messages.size());
        assertEquals(new JsonPrimitive(message), messages.get(0));
    }

    protected PushService getPushService() throws GeneralSecurityException {
        PushService pushService;

        if (!configuration.isVapid()) {
            pushService = new PushService(GCM_API_KEY);
        } else {
            pushService = new PushService(PUBLIC_KEY, PRIVATE_KEY, VAPID_SUBJECT);
        }
        return pushService;
    }

    /**
     * The name used by JUnit to display the test.
     *
     * @return
     */
    public String getDisplayName() {
        return "Browser " + configuration.browser + ", version " + configuration.version + ", vapid " + configuration.isVapid();
    }
}
