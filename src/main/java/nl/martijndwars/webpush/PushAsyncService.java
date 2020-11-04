package nl.martijndwars.webpush;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class PushAsyncService extends AbstractPushService<PushAsyncService> {

    private final AsyncHttpClient httpClient = asyncHttpClient();

    public PushAsyncService() {
    }

    public PushAsyncService(String gcmApiKey) {
        super(gcmApiKey);
    }

    public PushAsyncService(KeyPair keyPair) {
        super(keyPair);
    }

    public PushAsyncService(KeyPair keyPair, String subject) {
        super(keyPair, subject);
    }

    public PushAsyncService(String publicKey, String privateKey) throws GeneralSecurityException {
        super(publicKey, privateKey);
    }

    public PushAsyncService(String publicKey, String privateKey, String subject) throws GeneralSecurityException {
        super(publicKey, privateKey, subject);
    }

    /**
     * Send a notification asynchronously.
     *
     * @param notification
     * @param encoding
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws JoseException
     */
    public CompletableFuture<Response> send(Notification notification, Encoding encoding) throws GeneralSecurityException, IOException, JoseException {
        BoundRequestBuilder httpPost = preparePost(notification, encoding);
        return httpPost.execute().toCompletableFuture();
    }

    public CompletableFuture<Response> send(Notification notification) throws GeneralSecurityException, IOException, JoseException {
        return send(notification, Encoding.AES128GCM);
    }

    /**
     * Prepare a POST request for AHC.
     *
     * @param notification
     * @param encoding
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws JoseException
     */
    public BoundRequestBuilder preparePost(Notification notification, Encoding encoding) throws GeneralSecurityException, IOException, JoseException {
        HttpRequest request = prepareRequest(notification, encoding);
        BoundRequestBuilder httpPost = httpClient.preparePost(request.getUrl());
        request.getHeaders().forEach(httpPost::addHeader);
        if (request.getBody() != null) {
            httpPost.setBody(request.getBody());
        }
        return httpPost;
    }
}
