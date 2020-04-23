package nl.martijndwars.webpush;

import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Murat Karagözgil
 */
class PushServiceTest {

    private static final String endpoint = "https://fcm.googleapis.com/fcm/send/czhtwDhu7Lg:APA91bEdCfN8gIahOdwBf3GaZXrsBBcr6S4I3Z2l8YjKUHo0Usxpq5pC7tfKVx2P7e9UPOQpu6LmfvhLajUikSDepYYs8UKXjEqHQPzfWQKLCZCd94PvYG-nHZ09BmNZPGIIjhhken7t";
    private static final String publicKey = "BNyLse7yszy7mVoZNZgfSDxpoMi9umnzn2OJOr9FF_ZfBklzuuWthzLgevb5pNgrrqVg3DaTDy_MfPPCB3PL_m0=";
    private static final String p256dh = "BHupgDqUTZGNpbYY0rOQy8vAdFvQBR3V8ggrYr8YGoyUoXXJmS0IWjHOr9JmpP0bDh7qjI9Jyxs_GGKeqqnhxt8";
    private static final String privateKey = "AJ93_CR96aJjxJ_-qcXc0iXpqXKAWxMH07JP31Hgl1cy";
    private static final String authKey = "Efke3k7xbr-yXiGDd_WHuA";

    private Notification notification;
    private PushService pushService;

    @BeforeAll
    public static void addSecurityProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeEach
    public void setUp() throws GeneralSecurityException {
        this.notification = Notification.builder()
                .endpoint(endpoint)
                .userPublicKey(p256dh)
                .userAuth(authKey)
                .payload("Hello World")
                .ttl((int) Duration.ofDays(15).getSeconds())
                .build();

        this.pushService = new PushService(publicKey, privateKey);
    }

    @Test
    public void testCustomHttpClientWork() throws IOReactorException {
        pushService.setCustomHttpClient(getCustomClosableHttpClient());
        pushService.startCustomHttpClient();
        assertNotNull(pushService.getCustomHttpClient());
    }

    @Test
    public void testCustomHttpClientIsNull() {
        assertNull(pushService.getCustomHttpClient());
    }

    @Test
    public void testSendNotificationIsSuccessViaDefaultHttpClient() throws InterruptedException, GeneralSecurityException, JoseException, ExecutionException, IOException {
        HttpResponse response = pushService.send(notification);
        assertEquals(201, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testSendNotificationIsSuccessViaCustomHttpClient() throws InterruptedException, GeneralSecurityException, JoseException, ExecutionException, IOException {
        pushService.setCustomHttpClient(getCustomClosableHttpClient());
        pushService.startCustomHttpClient();
        HttpResponse response = pushService.send(notification);
        assertEquals(201, response.getStatusLine().getStatusCode());
    }

    private CloseableHttpAsyncClient getCustomClosableHttpClient() throws IOReactorException {
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
        PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(ioReactor);
        return HttpAsyncClients.custom().setMaxConnTotal(1).setConnectionManager(cm).build();
    }
}