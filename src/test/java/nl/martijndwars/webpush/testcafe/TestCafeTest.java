package nl.martijndwars.webpush.testcafe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestCafeTest {
  public static Webserver webserver;

  @BeforeAll
  public static void startWebserver() {
    webserver = new Webserver();
    webserver.start();
  }

  @Test
  public void testEndToEnd() throws InterruptedException {
    Thread.sleep(60 * 1000);
  }
}
