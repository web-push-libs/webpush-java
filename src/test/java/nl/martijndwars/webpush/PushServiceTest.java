package nl.martijndwars.webpush;

import java.nio.charset.StandardCharsets;
import java.security.Security;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

public class PushServiceTest {
    @BeforeClass
    public static void addSecurityProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testPushFirefoxVapid() throws Exception {
        String endpoint = "https://updates.push.services.mozilla.com/wpush/v1/gAAAAABX1ZgBNvDz6ZIAh6OqNh3hN4ZLEa57oS22mHI70mnvrDbIi-MnJu7FxFzvMV31L_AnIxP_p1Ot47KP8Xmit3XIQjZDjTahqBPmmntWX8JM6AtRxcAHxmXH6KqhyWwL1QEA0jBp";

        // Base64 string user public key/auth
        String userPublicKey = "BLLgHYo0xlN3GDSrz4g6SpTDLvJv+oFR0FSLLnncXFojvVyoOePpNXaUpsj4s/huAX7zb+qS1Lxo6qNLXNgWN7k=";
        String userAuth = "wkbtrbgITbb9qPBVOw3ftw==";

        // Base64 string server public/private key
        String vapidPublicKey = "BOH8nTQA5iZhl23+NCzGG9prvOZ5BE0MJXBW+GUkQIvRVTVB32JxmX0V1j6z0r7rnT7+bgi6f2g5fMPpAh5brqM=";
        String vapidPrivateKey = "TRlY/7yQzvqcLpgHQTxiU5fVzAAvAw/cdSh5kLFLNqg=";

        // Construct notification
        Notification notification = new Notification(endpoint, userPublicKey, userAuth, getPayload());

        // Construct push service
        PushService pushService = new PushService();
        pushService.setSubject("mailto:admin@martijndwars.nl");
        pushService.setPublicKey(Utils.loadPublicKey(vapidPublicKey));
        pushService.setPrivateKey(Utils.loadPrivateKey(vapidPrivateKey));

        // Send notification!
        HttpResponse httpResponse = pushService.send(notification);

        System.out.println(httpResponse.getStatusLine().getStatusCode());
        System.out.println(IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    }

    @Test
    public void testPushChromeVapid() throws Exception {
        String endpoint = "https://fcm.googleapis.com/fcm/send/fAAs_rrnDHQ:APA91bHlqjMZzphwP2xckJa9jL0CwtEvlLTL1OEfmRuwqviGLnqQTvMr4WLiwg7jElESXPLYO7qUc5mWvvv-bqs9lRenEbUSL2R191F-quyhE_fZ6JM3giqMQMhAEifDG-s5eHsRPQUG";

        // Base64 string user public key/auth
        String userPublicKey = "BM9qL254VsQlM8Zi6Hd0khUYSn8075A+td+/DZELdA2L173DIDz42NbjZC51NRfAuVaxh/vT/+UZr37S55EtY7k=";
        String userAuth = "KaiGaQKMyCW8qEk2NMJwjA==";

        // Base64 string server public/private key
        String vapidPublicKey = "BOH8nTQA5iZhl23+NCzGG9prvOZ5BE0MJXBW+GUkQIvRVTVB32JxmX0V1j6z0r7rnT7+bgi6f2g5fMPpAh5brqM=";
        String vapidPrivateKey = "TRlY/7yQzvqcLpgHQTxiU5fVzAAvAw/cdSh5kLFLNqg=";

        // Construct notification
        Notification notification = new Notification(endpoint, userPublicKey, userAuth, getPayload());

        // Construct push service
        PushService pushService = new PushService();
        pushService.setSubject("mailto:admin@martijndwars.nl");
        pushService.setPublicKey(Utils.loadPublicKey(vapidPublicKey));
        pushService.setPrivateKey(Utils.loadPrivateKey(vapidPrivateKey));

        // Send notification!
        HttpResponse httpResponse = pushService.send(notification);

        System.out.println(httpResponse.getStatusLine().getStatusCode());
        System.out.println(IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    }

    @Test
    public void testPushFirefox() throws Exception {
        String endpoint = "https://updates.push.services.mozilla.com/wpush/v1/gAAAAABX1Y_lvdzIpzBfRnceQdoNa_DiDy2OH7weXClk5ysidEuoPH8xv0Qq9ADFNTAB4e1TOuT50bbpN-bWVymBqy1b6Mecrz_SHf8Hvh620ViAbL5Zuyp5AqlA7i6g4BGX8h1H23zH";

        // Base64 string user public key/auth
        String userPublicKey = "BNYbTpyTEUFNK9BacT1rgpx7SXuKkLVKOF0LFnK8mLyPeW3SLk3nmXoPXSCkNKovcKChNxbG+q3mGW9J8JRg+6w=";
        String userAuth = "40SZaWpcvu55C+mlWxu0kA==";

        // Construct notification
        Notification notification = new Notification(endpoint, userPublicKey, userAuth, getPayload());

        // Construct push service
        PushService pushService = new PushService();

        // Send notification!
        HttpResponse httpResponse = pushService.send(notification);

        System.out.println(httpResponse.getStatusLine().getStatusCode());
        System.out.println(IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    }

    @Test
    public void testPushChrome() throws Exception {
        String endpoint = "https://android.googleapis.com/gcm/send/fIYEoSib764:APA91bGLILlBB9XnndQC-fWWM1D-Ji2reiVnRS-sM_kfHQyVssWadi6XRCfd9Dxf74fL6y3-Zaazohhl_W4MCLaqhdr5-WucacYjQS6B5-VyOwYQxzEkU2QABvUUxBcZw91SHYDGmkIt";

        // Base64 string user public key/auth
        String userPublicKey = "BA7JhUzMirCMHC94XO4ODFb7sYzZPMERp2AFfHLs1Hi1ghdvUfid8dlNseAsXD7LAF+J33X+ViRJ/APpW8cnrko=";
        String userAuth = "8wtwPHBdZ7LWY4p4WWJIzA==";

        // Construct notification
        Notification notification = new Notification(endpoint, userPublicKey, userAuth, getPayload());

        // Construct push service
        PushService pushService = new PushService();
        pushService.setGcmApiKey("AIzaSyDSa2bw0b0UGOmkZRw-dqHGQRI_JqpiHug");

        // Send notification!
        HttpResponse httpResponse = pushService.send(notification);

        System.out.println(httpResponse.getStatusLine().getStatusCode());
        System.out.println(IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    }

    @Test
    public void testSign() throws Exception {
        // Base64 string server public/private key
        String vapidPublicKey = "BOH8nTQA5iZhl23+NCzGG9prvOZ5BE0MJXBW+GUkQIvRVTVB32JxmX0V1j6z0r7rnT7+bgi6f2g5fMPpAh5brqM=";
        String vapidPrivateKey = "TRlY/7yQzvqcLpgHQTxiU5fVzAAvAw/cdSh5kLFLNqg=";

        JwtClaims claims = new JwtClaims();
        claims.setAudience("https://developer.services.mozilla.com/a476b8ea-c4b8-4359-832a-e2747b6ab88a");

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(Utils.loadPrivateKey(vapidPrivateKey));
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

        System.out.println(jws.getCompactSerialization());
    }

    /**
     * Some dummy payload (a JSON object)
     *
     * @return
     */
    private byte[] getPayload() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.append("title", "Hello");
        jsonObject.append("message", "World");

        return jsonObject.toString().getBytes();
    }
}
