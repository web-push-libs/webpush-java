package nl.martijndwars.webpush.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import nl.martijndwars.webpush.Subscription;

@Parameters(separators = "=", commandDescription = "Send a push notification")
public class SendNotificationCommand {
    @Parameter(names = "--endpoint", description = "The push  subscription URL.", required = true)
    private String endpoint;

    @Parameter(names = "--key", description = "The user public encryption key.", required = true)
    private String key;

    @Parameter(names = "--auth", description = "The user auth secret.", required = true)
    private String auth;

    @Parameter(names = "--publicKey", description = "The public key as base64url encoded string.", required = true)
    private String publicKey;

    @Parameter(names = "--privateKey", description = "The private key as base64url encoded string.", required = true)
    private String privateKey;

    @Parameter(names = "--payload", description = "The message to send.")
    private String payload = "Hello world";

    @Parameter(names = "--ttl", description = "The number of seconds that the push service should retain the message.")
    private int ttl;

    public Subscription getSubscription() {
        return new Subscription(endpoint, new Subscription.Keys(key, auth));
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPayload() {
        return payload;
    }

    public int getTtl() {
        return ttl;
    }
}
