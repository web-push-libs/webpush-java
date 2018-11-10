package nl.martijndwars.webpush;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;

public class PushTest {
    @Test
    public void testAes128Gcm() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        String publicKey = "BG15nGlC3qDleDkI6VPDVT4Ar28t2v2zcHthuRMyoU4nD-t1XpU-PUw8Ve9FH0Zrb0saJDwzi6AJYJAL_CmwNcw";
        String privateKey = "gNc81rawd_fKWXpFOhllPUD6BJLIkePus7lQ46jMvTs";

        Subscription subscription = new Gson().fromJson("{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/cZgR4jdX_a0:APA91bGtFF7ymruzk4OQak-Ck0phn_78WG5eU7ODdolIUB3bXlUUyH08j655HBlHZcRFkCywJRnP_lnFoBQ5WIiiUwdqXyG9ZQA72zV97c_h4i2m17CQcN_2Ycc8HtwDleSJCcsMAu_T\",\"expirationTime\":null,\"keys\":{\"p256dh\":\"BJ7H-eGuEwYMn2u4Rbue3YUvXfugPgEERt1nMW8DyJgFfXmAHgo0mZi5LfzHW65xvI4gYlCvL8qq1OzUks3IYzA\",\"auth\":\"05MiKXdBb159mUy6G84vaA\"}}", Subscription.class);
        Notification notification = new Notification(subscription, "Hello");

        PushService pushService = new PushService();
        pushService.setPublicKey(publicKey);
        pushService.setPrivateKey(privateKey);
        pushService.setSubject("mailto:admin@domain.com");

        HttpResponse httpResponse = pushService.send(notification, Encoding.AES128GCM);

        System.out.println(httpResponse.getStatusLine().getStatusCode());
        System.out.println(IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    }

    @Test
    public void companionPushTest() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        String publicKey = "BLzPK96e2_tX5pE9HA9D6j_H1fkZi3yEgpG1HGifioFtM1wWSoJBcV7vWAsXzIVngaVAm5lmnD2TwvF46ouYx0M";
        String privateKey = "GtGG-dCPNMi5nTw2jwsvHnTIGIeQudJW26uIoYgglDs";

        Subscription subscription = new Gson().fromJson("{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/fSQGGxFBNUo:APA91bH3iXE_ZzJwv5dM0d_fYRvPkKQCQnXt4QN039C6l5tNfDxtMl6rDlB52ocNWk5jkvwOLWOWmZAHeKzSxx915BfSVxI3Q91t60373tcerRpuUSV8UkXrGxVlACMbSQrcGmp87-Yd\",\"keys\":{\"p256dh\":\"BPOCrS9SJrd6-EQRbGhPY4o0asLnAx70gzZru24GMi98czQqpnyKYL9vuHVj4QV4NPRt7O4XxmsV9HfaQs0fZ3Y=\",\"auth\":\"XEdCH6NenHYosy1BDoF5yA==\"}}", Subscription.class);
        Notification notification = new Notification(subscription, "Hëllö World!");

        PushService pushService = new PushService();
        pushService.setPublicKey(publicKey);
        pushService.setPrivateKey(privateKey);
        pushService.setSubject("mailto:admin@domain.com");

        HttpResponse httpResponse = pushService.send(notification);

        System.out.println(httpResponse.getStatusLine().getStatusCode());
        System.out.println(IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    }

    @Test
    public void testPush() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Deserialize subscription object
        Subscription subscription = new Gson().fromJson(
                "{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/efI2iY2iI7g:APA91bFJMK9cNaCh9dDyQ8X3kuXEzVYlHGEJ2BLKG57n7H_NCjTyjJ87wczJKkAV8wfqo5iZRFnTJf1LgaqZ5NsNhGX2PTQQM5pPaCS41ogYfSY9KpfKZJTY410sUQG6yEDGjSuXrtbP\",\"keys\":{\"p256dh\":\"BHj7LOv2ARShKqY_RXP5zoSSpvAevF-VTzJFm9dXfTtnFg5wHVqei_74UOF8vr8kzY-3hR-wgdhGQOw10AxkmBI=\",\"auth\":\"cJN5ZAvblDfOo_Y_ibFZSg==\"}}",
                Subscription.class
        );

        // Construct notification
        Notification notification = new Notification(
                subscription.endpoint,
                subscription.keys.p256dh,
                subscription.keys.auth,
                "Hello, world!"
        );

        // Construct push service
        KeyPair keyPair = TestUtils.readVapidKeys();

        PushService pushService = new PushService();
        pushService.setKeyPair(keyPair);
        pushService.setSubject("mailto:admin@domain.com");

        // Send notification!
        HttpResponse httpResponse = pushService.send(notification);

        System.out.println(httpResponse.getStatusLine().getStatusCode());
        System.out.println(IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    }

    /**
     * PublicKey = 0x04,0xf7,0xf7,0xe9,0x00,0x2a,0x64,0xd3,0x6f,0xbe,0x79,0x7d,0x92,0x4a,0x53,0x0d,0xea,0xd7,0x50,0xeb,0x7a,0x36,0x6e,0x8f,0xe3,0x1f,0x46,0x12,0xf9,0x85,0x54,0x52,0xa0,0x57,0x20,0x95,0x5b,0x2d,0xb8,0x29,0x44,0xec,0xeb,0x02,0xd0,0xb5,0xb5,0x76,0x52,0x0f,0x35,0xba,0xd6,0xcd,0x9f,0x1f,0x3a,0xfe,0xf6,0x73,0x5c,0x3c,0xab,0x97,0xcf
     * PublicKey = 04f7f7e9002a64d36fbe797d924a530dead750eb7a366e8fe31f4612f9855452a05720955b2db82944eceb02d0b5b576520f35bad6cd9f1f3afef6735c3cab97cf
     * PublicKey = BPf36QAqZNNvvnl9kkpTDerXUOt6Nm6P4x9GEvmFVFKgVyCVWy24KUTs6wLQtbV2Ug81utbNnx86_vZzXDyrl88=
     *
     * PrivateKey = 1e161d06ce7320266af737d61e72e8010abbbd2f30cf87375077054efdd018f5
     * PrivateKey = HhYdBs5zICZq9zfWHnLoAQq7vS8wz4c3UHcFTv3QGPU=
     *
     * @throws Exception
     */
    @Test
    public void testPush2() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Deserialize subscription object
        Subscription subscription = new Gson().fromJson(
                "{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/crTeErRUPTc:APA91bFVWbWwmV5pT-ChX-lQRdR-e_WFB9TKlTgbrA7Ipq8s87pwgxtrSfmAItENo_uL6MDhv5n_G-HCqUR2YmgBF07dprhbwAsVOkpvv07H0CmYrMC7oss27oeIT5pUKbejBWQ1gcik\",\"keys\":{\"p256dh\":\"BOpf2C_Lt26VMbY9JfLCSEKfe3-MZ89KF3rDpZqBdweckBxvaw753hOj0ox5isqoBki8UgPox7FsgTCZ3CwDa5s=\",\"auth\":\"YupUeBKECwzdSwHNre11HA==\"}}",
                Subscription.class
        );

        // Construct notification
        Notification notification = new Notification(
                subscription.endpoint,
                subscription.keys.p256dh,
                subscription.keys.auth,
                "Hello, world!"
        );

        // Construct push service
        PublicKey publicKey = Utils.loadPublicKey("BPf36QAqZNNvvnl9kkpTDerXUOt6Nm6P4x9GEvmFVFKgVyCVWy24KUTs6wLQtbV2Ug81utbNnx86_vZzXDyrl88=");
        PrivateKey privateKey = Utils.loadPrivateKey("HhYdBs5zICZq9zfWHnLoAQq7vS8wz4c3UHcFTv3QGPU=");

        PushService pushService = new PushService();
        pushService.setPrivateKey(privateKey);
        pushService.setPublicKey(publicKey);
        pushService.setSubject("mailto:admin@domain.com");

        // Send notification!
        HttpResponse httpResponse = pushService.send(notification);

        System.out.println(httpResponse.getStatusLine().getStatusCode());
        System.out.println(IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    }
}
