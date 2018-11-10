# 4.0.0

* Support [aes128gcm content encoding](https://tools.ietf.org/html/draft-ietf-httpbis-encryption-encoding-09#section-2) (#72)
  * Use `PushService.send(Notification, Encoding)` or the analogous `sendAsync` with `Encoding.AES128GCM`.
* Remove Guava dependency (#69)

