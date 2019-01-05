# 5.0.1

* Bugfix: Only verify the VAPID key pair if the keys are actually present (fixes #73).
* Improvement: Add test configurations for GCM-only to the selenium test suite.

# 5.0.0

* Use aes128gcm as the default encoding (#75).
* Remove BouncyCastle JAR from source and let Gradle put together the class path for the CLI.

# 4.0.0

* Support [aes128gcm content encoding](https://tools.ietf.org/html/draft-ietf-httpbis-encryption-encoding-09#section-2) (#72)
  * Use `PushService.send(Notification, Encoding)` or the analogous `sendAsync` with `Encoding.AES128GCM`.
* Remove Guava dependency (#69)

