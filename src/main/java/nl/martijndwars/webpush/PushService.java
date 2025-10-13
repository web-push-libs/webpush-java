package nl.martijndwars.webpush;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.*;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PushService {

    private final HttpClient client;
    private final KeyPair vapidKeyPair;
    private final String publicKeyBase64;
    private final String subject;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public PushService(String publicKeyBase64, String privateKeyBase64, String subject) throws GeneralSecurityException {
        this.client = HttpClient.newHttpClient();
        this.subject = subject;
        this.publicKeyBase64 = publicKeyBase64;

        KeyFactory kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        byte[] pubBytes = Base64.getUrlDecoder().decode(publicKeyBase64);
        byte[] privBytes = Base64.getUrlDecoder().decode(privateKeyBase64);

        PublicKey pubKey = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(pubBytes));
        PrivateKey privKey = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privBytes));
        this.vapidKeyPair = new KeyPair(pubKey, privKey);
    }

    /**
     * Enviar notificación de forma síncrona
     */
    public HttpResponse<byte[]> send(Notification notification) throws IOException, InterruptedException, JoseException, GeneralSecurityException {
        HttpRequest request = prepareRequest(notification);
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Enviar notificación de forma asincrónica
     */
    public CompletableFuture<HttpResponse<byte[]>> sendAsync(Notification notification) throws JoseException, GeneralSecurityException {
        HttpRequest request = prepareRequest(notification);
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Preparar HttpRequest incluyendo headers de la suscripción y JWT VAPID
     */
    private HttpRequest prepareRequest(Notification notification) throws JoseException, GeneralSecurityException {
        String endpoint = notification.getEndpoint(); // si tu fork lo tiene
        byte[] payload = notification.getPayload();

        String jwt = createVapidJWT(URI.create(endpoint));

        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("TTL", "60")
                .header("Authorization", "vapid t=" + jwt + ", k=" + publicKeyBase64)
                .build();
    }

    /**
     * Crear JWT VAPID firmado con la clave privada usando BouncyCastle
     */
    private String createVapidJWT(URI endpoint) throws JoseException {
        JwtClaims claims = new JwtClaims();
        claims.setAudience(endpoint.getScheme() + "://" + endpoint.getHost());
        claims.setExpirationTimeMinutesInTheFuture(12 * 60); // 12h
        claims.setSubject(subject);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
        jws.setKey(vapidKeyPair.getPrivate());
        jws.setHeader("typ", "JWT");

        return jws.getCompactSerialization();
    }

    public KeyPair getVapidKeyPair() {
        return vapidKeyPair;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }
}
