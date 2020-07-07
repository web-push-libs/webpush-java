package nl.martijndwars.webpush.testcafe;

import java.nio.file.Paths;
import java.security.Security;

import com.google.gson.Gson;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.resource.PathResourceManager;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Webserver {
  protected final Undertow server;

  public static void main(String[] args) throws InterruptedException {
    Webserver webserver = new Webserver();
    webserver.start();

    Thread.sleep(600 * 1000);
  }

  public Webserver() {
    this.server = Undertow.builder()
        .addHttpListener(5000, "localhost")
        .setHandler(getHandler())
        .build();
  }

  public void start() {
    server.start();
  }

  protected HttpHandler getHandler() {
    return Handlers.predicate(
        Predicates.suffixes(".html", ".js"),
        Handlers.resource(new PathResourceManager(Paths.get("src/test/resources/static"))),
        Handlers
            .path(Handlers.redirect("/index.html"))
            .addPrefixPath("/send", getSendHandler())
    );
  }

  protected HttpHandler getSendHandler() {
    return exchange -> {
      FormDataParser parser = FormParserFactory.builder().build().createParser(exchange);
      MyHandler handler = new MyHandler();
      parser.parse(handler);
    };
  }

  private static class MyHandler implements HttpHandler{
    final String PUBLIC_KEY = "BAPGG2IY3Vn48d_H8QNuVLRErkBI0L7oDOOCAMUBqYMTMTzukaIAuB5OOcmkdeRICcyQocEwD-oxVc81YXXZPRY";
    final String PRIVATE_KEY = "A7xDGuxMZ4ufflcAhBW23xpoWZNOLwM4Rw2wXjP0y6M";
    final String SUBJECT = "Foobarbaz";
    final String PAYLOAD = "My fancy message";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
      FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);
      String subscriptionJson = formData.get("subscriptionJson").getFirst().getValue();
      System.out.println(subscriptionJson);

      Security.addProvider(new BouncyCastleProvider());
      PushService pushService = new PushService(PUBLIC_KEY, PRIVATE_KEY, SUBJECT);
      Subscription subscription = new Gson().fromJson(subscriptionJson, Subscription.class);
      Notification notification = new Notification(subscription, PAYLOAD);
      HttpResponse httpResponse = pushService.send(notification);
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      System.out.println(statusCode);
    }
  }
}
