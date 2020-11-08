# WebPush

A Web Push library for Java 8. Supports payloads and VAPID.

[![Build Status](https://travis-ci.org/web-push-libs/webpush-java.svg?branch=master)](https://travis-ci.org/web-push-libs/webpush-java)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/nl.martijndwars/web-push/badge.svg)](https://search.maven.org/search?q=g:nl.martijndwars%20AND%20a:web-push)

## Installation

For Gradle, add the following dependency to `build.gradle`:

```groovy
compile group: 'nl.martijndwars', name: 'web-push', version: '5.1.1'
```

For Maven, add the following dependency to `pom.xml`:

```xml
<dependency>
    <groupId>nl.martijndwars</groupId>
    <artifactId>web-push</artifactId>
    <version>5.1.1</version>
</dependency>
```

This library depends on BouncyCastle, which acts as a Java Cryptography Extension (JCE) provider. BouncyCastle's JARs are signed, and depending on how you package your application, you may need to include BouncyCastle yourself as well.

## Building

To assemble all archives in the project:

```sh
./gradlew assemble
```

## Usage

This library is meant to be used as a Java API. However, it also exposes a CLI to easily generate a VAPID keypair and send a push notification.

### CLI

A command-line interface is available to easily generate a keypair (for VAPID) and to try sending a notification.

```
$ ./gradlew run
Usage: <main class> [command] [command options]
  Commands:
    generate-key      Generate a VAPID keypair
      Usage: generate-key

    send-notification      Send a push notification
      Usage: send-notification [options]
        Options:
          --subscription
            A subscription in JSON format.
          --publicKey
            The public key as base64url encoded string.
          --privateKey
            The private key as base64url encoded string.
          --payload
            The message to send.
            Default: Hello, world!
          --ttl
            The number of seconds that the push service should retain the message.

```

For example, to generate a keypair and output the keys in base64url encoding:

```
$ ./gradlew run --args="generate-key"
PublicKey:
BGgL7I82SAQM78oyGwaJdrQFhVfZqL9h4Y18BLtgJQ-9pSGXwxqAWQudqmcv41RcWgk1ssUeItv4-8khxbhYveM=

PrivateKey:
ANlfcVVFB4JiMYcI74_h9h04QZ1Ks96AyEa1yrMgDwn3
```

Use the public key in the call to `pushManager.subscribe` to get a subscription. Then, to send a notification:

```
$ ./gradlew run --args='send-notification --endpoint="https://fcm.googleapis.com/fcm/send/fH-M3xRoLms:APA91bGB0rkNdxTFsXaJGyyyY7LtEmtHJXy8EqW48zSssxDXXACWCvc9eXjBVU54nrBkARTj4Xvl303PoNc0_rwAMrY9dvkQzi9fkaKLP0vlwoB0uqKygPeL77Y19VYHbj_v_FolUlHa" --key="BOtBVgsHVWXzwhDAoFE8P2IgQvabz_tuJjIlNacmS3XZ3fRDuVWiBp8bPR3vHCA78edquclcXXYb-olcj3QtIZ4=" --auth="IOScBh9LW5mJ_K2JwXyNqQ==" --publicKey="BGgL7I82SAQM78oyGwaJdrQFhVfZqL9h4Y18BLtgJQ-9pSGXwxqAWQudqmcv41RcWgk1ssUeItv4-8khxbhYveM=" --privateKey="ANlfcVVFB4JiMYcI74_h9h04QZ1Ks96AyEa1yrMgDwn3" --payload="Hello world"'
```

#### Proxy

If you are behind a corporate proxy you may need to specify the proxy host. This library respects [Java's Network Properties](https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html), which means that you can pass `https.proxyHost` and `http.proxyPort` when invoking `java`, e.g. `java -Dhttp.proxyHost=proxy.corp.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=proxy.corp.com -Dhttps.proxyPort=443 -jar ...`.

### API

First, make sure you add the BouncyCastle security provider:

```java
Security.addProvider(new BouncyCastleProvider());
```

Then, create an instance of the push service, either `nl.martijndwars.webpush.PushService` for synchronous blocking HTTP calls, or `nl.martijndwars.webpush.PushAsyncService` for asynchronous non-blocking HTTP calls:

```java
PushService pushService = new PushService(...);
```

Then, create a notification based on the user's subscription:

```java
Notification notification = new Notification(...);
```

To send a push notification:

```java
pushService.send(notification);
```

See [wiki/Usage-Example](https://github.com/web-push-libs/webpush-java/wiki/Usage-Example)
for detailed usage instructions. If you plan on using VAPID, read [wiki/VAPID](https://github.com/web-push-libs/webpush-java/wiki/VAPID).

## Testing

The integration tests use [Web Push Testing Service (WPTS)](https://github.com/GoogleChromeLabs/web-push-testing-service) to handle the Selenium and browser orchestrating. We use a forked version that fixes a bug on macOS. To install WPTS:

```
npm i -g github:MartijnDwars/web-push-testing-service#bump-selenium-assistant
```

Then start WPTS:

```
web-push-testing-service start wpts
```

Then run the tests:

```
./gradlew clean test
```

Finally, stop WPTS:

```
web-push-testing-service stop wpts
```

## FAQ

### Why does encryption take multiple seconds?

There may not be enough entropy to generate a random seed, which is common on headless servers. There exist two ways to overcome this problem:

- Install [haveged](http://stackoverflow.com/a/31208558/368220), a _"random number generator that remedies low-entropy conditions in the Linux random device that can occur under some workloads, especially on headless servers."_ [This](https://www.digitalocean.com/community/tutorials/how-to-setup-additional-entropy-for-cloud-servers-using-haveged) tutorial explains how to install haveged on different Linux distributions.

- Change the source for random number generation in the JVM from `/dev/random` to `/dev/urandom`. [This](https://docs.oracle.com/cd/E13209_01/wlcp/wlss30/configwlss/jvmrand.html) page offers some explanation.

## Credit

To give credit where credit is due, the PushService is mostly a Java port of marco-c/web-push. The HttpEce class is mostly a Java port of martinthomson/encrypted-content-encoding.

## Resources

### Specifications

- [Generic Event Delivery Using HTTP Push](https://tools.ietf.org/html/draft-ietf-webpush-protocol-11)
- [Message Encryption for Web Push](https://tools.ietf.org/html/draft-ietf-webpush-encryption-08)
- [Encrypted Content-Encoding for HTTP](https://tools.ietf.org/html/draft-ietf-httpbis-encryption-encoding-02)

### Miscellaneous

- [Voluntary Application Server Identification for Web Push](https://tools.ietf.org/html/draft-ietf-webpush-vapid-01)
- [Web Push Book](https://web-push-book.gauntface.com/)
- [Simple Push Demo](https://gauntface.github.io/simple-push-demo/)
- [Web Push: Data Encryption Test Page](https://jrconlin.github.io/WebPushDataTestPage/)
- [Push Companion](https://web-push-codelab.appspot.com/)

## Related

The web-push-libs organization hosts implementations of the Web Push protocol in several languages:

- For PHP, see [web-push-libs/web-push-php](https://github.com/web-push-libs/web-push-php)
- For NodeJS, see [web-push-libs/web-push](https://github.com/web-push-libs/web-push)
- For Python, see [web-push-libs/pywebpush](https://github.com/web-push-libs/pywebpush)
- For C#, see [web-push-libs/web-push-csharp](https://github.com/web-push-libs/web-push-csharp)
- For Scala, see [zivver/web-push](https://github.com/zivver/web-push)

