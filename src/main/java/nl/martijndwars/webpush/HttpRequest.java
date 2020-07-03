package nl.martijndwars.webpush;

import java.util.Map;

public class HttpRequest {

    private final String url;

    private final Map<String, String> headers;

    private final byte[] body;

    public HttpRequest(String url, Map<String, String> headers, byte[] body) {
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

}
