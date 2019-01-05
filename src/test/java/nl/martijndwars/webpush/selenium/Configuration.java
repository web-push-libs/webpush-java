package nl.martijndwars.webpush.selenium;

public class Configuration {
    protected final String browser;
    protected final String version;
    protected final String publicKey;

    Configuration(String browser, String version, String publicKey) {
        this.browser = browser;
        this.version = version;
        this.publicKey = publicKey;
    }

    public boolean isVapid() {
        return !publicKey.isEmpty();
    }

    @Override
    public String toString() {
        return browser + ", " + version + ", " + publicKey;
    }
}
