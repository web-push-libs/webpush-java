package nl.martijndwars.webpush.selenium;

public class Configuration {
    protected final String browser;
    protected final String version;
    protected final String publicKey;
    protected final String gcmSenderId;

    Configuration(String browser, String version, String publicKey, String gcmSenderId) {
        this.browser = browser;
        this.version = version;
        this.publicKey = publicKey;
        this.gcmSenderId = gcmSenderId;
    }

    public boolean isVapid() {
        return publicKey != null && !publicKey.isEmpty();
    }

    @Override
    public String toString() {
        return browser + ", " + version + ", " + publicKey;
    }
}
