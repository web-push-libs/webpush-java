# How Push API works

In order to use push notifications, it is first neccesary to understand how
these work. The diagram below explains the interaction between the user's
browser, our server, and the browser manufacturer's push server.

                            (3)
    Third Party Push Server <-------------------- Your server
     (1) ^   \                                      ^ (2)
          \   \                                    /
           \   \                                  /
            \   \                                /
             \   v (4)                          /
           End User's web browser --------------


 1. First the end user's web browser needs to establish a push channel with the
    browser manufacturer's push server. In the case of Firefox, this would be
    a Mozilla server, in Chrome's case, this would be a Google server. After
    this is done, a unique endpoint URL is sent to the browser, and the browser
    generates a public and private key pair which is stored internally in the
    browser. The browser then makes a  public key and a user authentication 
    secret used by your server to E2E encrypt messages to the user's browser.
 2. The browser sends the public key, authentication secret and endpoint URL to
    your server, and the server stores this somehow (in a database, in memory,
    a file, whatever). 
 3. When the time comes that the server wishes to send a push message, it
    retrieves the stored information on the push message subscription and
    creates an encrypted message with the public key and user authentication.
    Then the server contacts the endpoint URL and tells it to push some content
    to the user agent.
 4. Given everything looks OK, the push server pushes the message to the user's
    browser.


# Example of library usage

This needs to happen in three stages. First, we need to set up a
[serviceworker](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API)
in the user agent (browser), register it for push notifications, and send the
nececcary information to our server. This needs to be done in a service worker,
because service workers run in the background, even when the website is closed
by the user. This enables push notifications to be delivered, even when the user
has left the page (which is what we want, in most cases).

Next, we need to parse the input sent to the server. And then finally we can use
this library to send encrypted notifications. This section shows a basic example
on how to do these three steps.

## Client side

This example uses plain javascript to create a service worker and subcribe to
push notifications. This is done in a progressive way, so that the subscription
will only be done if the user agent supports it. If the user agent does not
support any of the required APIs (
[pushManager](https://developer.mozilla.org/en-US/docs/Web/API/PushManager), 
[serviceworker](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API)
 and [notifications](https://developer.mozilla.org/en-US/docs/Web/API/notification/Notification)
), then an error will be logged, but no breakage will occur. Thus users of
modern browsers can enjoy modern features, while users forced to use older
browsers will not be experiencing any broken features.

Note that we here assume you have set up a `sw.js` file with some handling of
the push messages you will receive. For an example on how to do so, see
[this page](https://github.com/gauntface/simple-push-demo/blob/master/src/service-worker.js).

```javascript

/**
 * Step one: run a function on load (or whenever is appropriate for you)
 * Function run on load sets up the service worker if it is supported in the
 * browser. Requires a serviceworker in a `sw.js`. This file contains what will
 * happen when we receive a push notification.
 * If you are using webpack, see the section below.
 */
$(function () {
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/sw.js').then(initialiseState);
    } else {
        console.warn('Service workers are not supported in this browser.');
    }
});

/**
 * Step two: The serviceworker is registered (started) in the browser. Now we
 * need to check if push messages and notifications are supported in the browser
 */
function initialiseState() {

    // Check if desktop notifications are supported
    if (!('showNotification' in ServiceWorkerRegistration.prototype)) {
        console.warn('Notifications aren\'t supported.');
        return;
    }

    // Check if user has disabled notifications
    // If a user has manually disabled notifications in his/her browser for 
    // your page previously, they will need to MANUALLY go in and turn the
    // permission back on. In this statement you could show some UI element 
    // telling the user how to do so.
    if (Notification.permission === 'denied') {
        console.warn('The user has blocked notifications.');
        return;
    }

    // Check is push API is supported
    if (!('PushManager' in window)) {
        console.warn('Push messaging isn\'t supported.');
        return;
    }

    navigator.serviceWorker.ready.then(function (serviceWorkerRegistration) {

        // Get the push notification subscription object
        serviceWorkerRegistration.pushManager.getSubscription().then(function (subscription) {

            // If this is the user's first visit we need to set up
            // a subscription to push notifications
            if (!subscription) {
                subscribe();

                return;
            }

            // Update the server state with the new subscription
            sendSubscriptionToServer(subscription);
        })
        .catch(function(err) {
            // Handle the error - show a notification in the GUI
            console.warn('Error during getSubscription()', err);
        });
    });
}

/**
 * Step three: Create a subscription. Contact the third party push server (for
 * example mozilla's push server) and generate a unique subscription for the
 * current browser.
 */
function subscribe() {
    navigator.serviceWorker.ready.then(function (serviceWorkerRegistration) {

        // Contact the third party push server. Which one is contacted by
        // pushManager is  configured internally in the browser, so we don't
        // need to worry about browser differences here.
        //
        // When .subscribe() is invoked, a notification will be shown in the
        // user's browser, asking the user to accept push notifications from
        // <yoursite.com>. This is why it is async and requires a catch.
        serviceWorkerRegistration.pushManager.subscribe({userVisibleOnly: true}).then(function (subscription) {

            // Update the server state with the new subscription
            return sendSubscriptionToServer(subscription);
        })
        .catch(function (e) {
            if (Notification.permission === 'denied') {
                console.warn('Permission for Notifications was denied');
            } else {
                console.error('Unable to subscribe to push.', e);
            }
        });
    });
}

/**
 * Step four: Send the generated subscription object to our server.
 */
function sendSubscriptionToServer(subscription) {

    // Get public key and user auth from the subscription object
    var key = subscription.getKey ? subscription.getKey('p256dh') : '';
    var auth = subscription.getKey ? subscription.getKey('auth') : '';

    // This example uses the new fetch API. This is not supported in all
    // browsers yet.
    return fetch('/profile/subscription', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            endpoint: subscription.endpoint,
            // Take byte[] and turn it into a base64 encoded string suitable for
            // POSTing to a server over HTTP
            key: key ? btoa(String.fromCharCode.apply(null, new Uint8Array(key))) : '',
            auth: auth ? btoa(String.fromCharCode.apply(null, new Uint8Array(auth))) : ''
        })
    });
}
```

After this code has been run successfully, the following JSON will be POSTed to
your server:

```
{
  "endpoint": "https://push.server.mozilla.org/unique-endpoint",
  "key": "TmljZSB0cnksIG5vIGtleSBmb3IgeW91IQ==",
  "auth": "Tm8hIEJhZCBoYWNrZXIh"
}
```

 - endpoint: The browser provider's push server endpoint.
 - key: The Base64 encoded public key of the browser's push subscription used to encrypt messages.
 - auth: A Base63 encoded authentication secret that used in authentication of messages.

### Webpack

If you are using webpack, you will probably want to do things a bit differently.
Since you are going to be building a single bundle fine, it is not practical to
always have a `sw.js` file accessible. You can use [serviceworker-loader](https://github.com/markdalgleish/serviceworker-loader)
to simply `require` the file as you would with any other file.

```javascript
var registerServiceWorker = require('serviceworker!./sw.js');

registerServiceWorker({ scope: '/' }).then(initialiseState);
```

## Serverside - Parse the input

Okay! Finally we are back in java-land. Now, assuming we have set up an endpoint
which accepts POST requests, we can create a subscription object from the input
we have received from the user agent.

This example uses the [Bouncy Castle](https://www.bouncycastle.org/java.html)
java cryptography APIs to parse the input from the user agent into a `PublicKey`
object.

```java
class Subscription {
    // Getters and setters for e.g. endpoint

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getAuth() {
        return auth;
    }

    /**
     * Returns the base64 encoded auth string as a byte[]
     */
    public byte[] getAuthAsBytes() {
        return Base64.getDecoder().decode(getAuth());
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns the base64 encoded public key string as a byte[]
     */
    public byte[] getKeyAsBytes() {
        return Base64.getDecoder().decode(getKey());
    }

    /**
     * Returns the base64 encoded public key as a PublicKey object
     */
    public PublicKey getUserPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyFactory kf = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECPoint point = ecSpec.getCurve().decodePoint(getKeyAsBytes());
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, ecSpec);

        return kf.generatePublic(pubSpec);
    }

    public void setEndpoint(String endpoint) {
		    this.endpoint = endpoint;
	  }
	
	  public String getEndpoint() {
		    return endpoint;
    }
}
```

If you are using encountering a `InvalidKeyException: Not an EC key: ECDH`, you
may have to add the following somewhere in yor code. This example adds it to the
Subscription object's constructor.

```java
public Subscription() {
  // Add BouncyCastle as an algorithm provider
  if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
  }
}
```

Now we need to take this `Subscription` object and save it somewhere so that we
can use it later when we need to send push notifications.

## Serverside - Send push messages

Now that we need to send push notifications, we cah finally use this library!
Woho! First, we need to decide if we want to send a Google Cloud Messaging (GCM)
message, or a Push API message. How this is done is up to you.

```java
/** The Time to live of GCM notifications */
private static final int TTL = 255;

public void sendPushMessage(Subscription sub, byte[] payload) {

  // Figure out if we should use GCM for this notification somehow
  boolean useGcm = shouldUseGcm(sub);
  Notification notification;
  PushService pushService;

  if (!useGcm) {
    // Create a notification with the endpoint, userPublicKey from the subscription and a custom payload
    notification = new Notification(
      sub.getEndpoint(),
      sub.getUserPublicKey(),
      sub.getAuthAsBytes(),
      payload
    );

    // Instantiate the push service, no need to use an API key for Push API
    pushService = new PushService();
  } else {
    // Or create a GcmNotification, in case of Google Cloud Messaging
    notification = new Notification(
      sub.getEndpoint(),
      sub.getUserPublicKey(),
      sub.getAuthAsBytes(),
      payload,
      TTL
    );

    // Instantiate the push service with a GCM API key
    pushService = new PushService("gcm-api-key");
  }

  // Send the notification
  pushService.send(notification);
}
```

Shortly after this code is run, the message should be delivered to the user.
Congratulations, you have just implemented push notifications in a progressive
way!

       Yay!
      /
    \o/
     |
    / \

