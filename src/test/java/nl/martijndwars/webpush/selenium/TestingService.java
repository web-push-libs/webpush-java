package nl.martijndwars.webpush.selenium;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TestingService {

    private final String baseUrl;
    private final HttpClient client;

    public TestingService(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.client = HttpClient.newHttpClient();
    }

    public int startTestSuite() throws IOException, InterruptedException {
        String response = request("start-test-suite/", null);
        JsonObject data = getData(response);
        return data.get("testSuiteId").getAsInt();
    }

    public JsonObject getSubscription(int testSuiteId, Configuration configuration) throws IOException, InterruptedException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("testSuiteId", testSuiteId);
        jsonObject.addProperty("browserName", configuration.browser);
        jsonObject.addProperty("browserVersion", configuration.version);

        if (configuration.gcmSenderId != null) {
            jsonObject.addProperty("gcmSenderId", configuration.gcmSenderId);
        }
        if (configuration.publicKey != null) {
            jsonObject.addProperty("vapidPublicKey", configuration.publicKey);
        }

        String response = request("get-subscription/", jsonObject.toString());
        return getData(response);
    }

    public JsonArray getNotificationStatus(int testSuiteId, int testId) throws IOException, InterruptedException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("testSuiteId", testSuiteId);
        jsonObject.addProperty("testId", testId);

        String response = request("get-notification-status/", jsonObject.toString());
        return getData(response).get("messages").getAsJsonArray();
    }

    public void endTestSuite(int testSuiteId) throws IOException, InterruptedException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("testSuiteId", testSuiteId);

        String response = request("end-test-suite/", jsonObject.toString());
        getData(response).get("success").getAsBoolean();
    }

    private String request(String endpoint, String jsonBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json");

        if (jsonBody != null) {
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " error for " + endpoint + ": " + response.body());
        }

        return response.body();
    }

    private JsonObject getData(String response) {
        JsonObject root = JsonParser.parseString(response).getAsJsonObject();
        return root.getAsJsonObject("data");
    }
}
