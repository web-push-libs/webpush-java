package nl.martijndwars.webpush;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static nl.martijndwars.webpush.Utils.*;

/**
 * An implementation of Encrypted Content-Encoding for HTTP.
 *
 * The first implementation follows the specification in [1]. The specification later moved from
 * "aesgcm" to "aes128gcm" as content encoding [2]. To remain backwards compatible this library
 * supports both.
 *
 * [1] https://tools.ietf.org/html/draft-ietf-httpbis-encryption-encoding-01
 * [2] https://tools.ietf.org/html/draft-ietf-httpbis-encryption-encoding-09
 *
 * TODO: Support multiple records (not needed for Web Push)
 */
public class HttpEce {
    public static final int KEY_LENGTH = 16;
    public static final int SHA_256_LENGTH = 32;
    public static final int TAG_SIZE = 16;
    public static final int TWO_BYTE_MAX = 65_536;
    public static final String WEB_PUSH_INFO = "WebPush: info\0";

    private Map<String, KeyPair> keys;
    private Map<String, String> labels;

    public HttpEce() {
        this(new HashMap<String, KeyPair>(), new HashMap<String, String>());
    }

    public HttpEce(Map<String, KeyPair> keys, Map<String, String> labels) {
        this.keys = keys;
        this.labels = labels;
    }

    /**
     * Encrypt the given plaintext.
     *
     * @param plaintext    Payload to encrypt.
     * @param salt       A random 16-byte buffer
     * @param privateKey A private key to encrypt this message with (Web Push: the local private key)
     * @param keyid      An identifier for the local key. Only applies to AESGCM. For AES128GCM, the header contains the keyid.
     * @param dh         An Elliptic curve Diffie-Hellman public privateKey on the P-256 curve (Web Push: the user's keys.p256dh)
     * @param authSecret An authentication secret (Web Push: the user's keys.auth)
     * @param version
     * @return
     * @throws GeneralSecurityException
     */
    public byte[] encrypt(byte[] plaintext, byte[] salt, byte[] privateKey, String keyid, ECPublicKey dh, byte[] authSecret, Encoding version) throws GeneralSecurityException {
        log("encrypt", plaintext);

        byte[][] keyAndNonce = deriveKeyAndNonce(salt, privateKey, keyid, dh, authSecret, version, ENCRYPT_MODE);
        byte[] key = keyAndNonce[0];
        byte[] nonce = keyAndNonce[1];

        // Note: Cipher adds the tag to the end of the ciphertext
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        GCMParameterSpec params = new GCMParameterSpec(TAG_SIZE * 8, nonce);
        cipher.init(ENCRYPT_MODE, new SecretKeySpec(key, "AES"), params);

        // For AES128GCM suffix {0x02}, for AESGCM prefix {0x00, 0x00}.
        if (version == Encoding.AES128GCM) {
            byte[] header = buildHeader(salt, keyid);
            log("header", header);

            byte[] padding = new byte[] { 2 };
            log("padding", padding);

            byte[][] encrypted = {cipher.update(plaintext), cipher.update(padding), cipher.doFinal()};
            log("encrypted", concat(encrypted));

            return log("ciphertext", concat(header, concat(encrypted)));
        } else {
            return concat(cipher.update(new byte[2]), cipher.doFinal(plaintext));
        }
    }

    /**
     * Decrypt the payload.
     *
     * @param payload Header and body (ciphertext)
     * @param salt    May be null when version is AES128GCM; the salt is extracted from the header.
     * @param version AES128GCM or AESGCM.
     * @return
     */
    public byte[] decrypt(byte[] payload, byte[] salt, byte[] key, String keyid, Encoding version) throws InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidAlgorithmParameterException, BadPaddingException, NoSuchProviderException, NoSuchPaddingException {
        byte[] body;

        // Parse and strip the header
        if (version == Encoding.AES128GCM) {
            byte[][] header = parseHeader(payload);

            salt = header[0];
            keyid = new String(header[2]);
            body = header[3];
        } else {
            body = payload;
        }

        // Derive key and nonce.
        byte[][] keyAndNonce = deriveKeyAndNonce(salt, key, keyid, null, null, version, DECRYPT_MODE);

        return decryptRecord(body, keyAndNonce[0], keyAndNonce[1], version);
    }

    public byte[][] parseHeader(byte[] payload) {
        byte[] salt = Arrays.copyOfRange(payload, 0, KEY_LENGTH);
        byte[] recordSize = Arrays.copyOfRange(payload, KEY_LENGTH, 20);
        int keyIdLength = Arrays.copyOfRange(payload, 20, 21)[0];
        byte[] keyId = Arrays.copyOfRange(payload, 21, 21 + keyIdLength);
        byte[] body = Arrays.copyOfRange(payload, 21 + keyIdLength, payload.length);

        return new byte[][] {
                salt,
                recordSize,
                keyId,
                body
        };
    }

    public byte[] decryptRecord(byte[] ciphertext, byte[] key, byte[] nonce, Encoding version) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        GCMParameterSpec params = new GCMParameterSpec(TAG_SIZE * 8, nonce);
        cipher.init(DECRYPT_MODE, new SecretKeySpec(key, "AES"), params);

        byte[] plaintext = cipher.doFinal(ciphertext);

        if (version == Encoding.AES128GCM) {
            // Remove one byte of padding at the end
            return Arrays.copyOfRange(plaintext, 0, plaintext.length - 1);
        } else {
            // Remove two bytes of padding at the start
            return Arrays.copyOfRange(plaintext, 2, plaintext.length);
        }
    }

    /**
     * Compute the Encryption Content Coding Header.
     *
     * See https://tools.ietf.org/html/draft-ietf-httpbis-encryption-encoding-09#section-2.1.
     *
     * @param salt  Array of 16 bytes
     * @param keyid
     * @return
     */
    private byte[] buildHeader(byte[] salt, String keyid) {
        byte[] keyIdBytes;

        if (keyid == null) {
            keyIdBytes = new byte[0];
        } else {
            keyIdBytes = encode(getPublicKey(keyid));
        }

        if (keyIdBytes.length > 255) {
            throw new IllegalArgumentException("They keyid is too large.");
        }

        byte[] rs = toByteArray(4096, 4);
        byte[] idlen = new byte[] { (byte) keyIdBytes.length };

        return concat(salt, rs, idlen, keyIdBytes);
    }

    /**
     * Future versions might require a null-terminated info string?
     *
     * @param type
     * @return
     */
    protected static byte[] buildInfo(String type, byte[] context) {
        ByteBuffer buffer = ByteBuffer.allocate(19 + type.length() + context.length);

        buffer.put("Content-Encoding: ".getBytes(UTF_8), 0, 18);
        buffer.put(type.getBytes(UTF_8), 0, type.length());
        buffer.put(new byte[1], 0, 1);
        buffer.put(context, 0, context.length);

        return buffer.array();
    }

    /**
     * Convenience method for computing the HMAC Key Derivation Function. The real work is offloaded to BouncyCastle.
     */
    protected static byte[] hkdfExpand(byte[] ikm, byte[] salt, byte[] info, int length) {
        log("salt", salt);
        log("ikm", ikm);
        log("info", info);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(ikm, salt, info));

        byte[] okm = new byte[length];
        hkdf.generateBytes(okm, 0, length);

        log("expand", okm);

        return okm;
    }

    public byte[][] extractSecretAndContext(byte[] key, String keyId, ECPublicKey dh, byte[] authSecret) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] secret = null;
        byte[] context = null;

        if (key != null) {
            secret = key;
            if (secret.length != KEY_LENGTH) {
                throw new IllegalStateException("An explicit key must be " + KEY_LENGTH + " bytes.");
            }
        } else if (dh != null) {
            byte[][] bytes = extractDH(keyId, dh);
            secret = bytes[0];
            context = bytes[1];
        } else if (keyId != null) {
            secret = keys.get(keyId).getPublic().getEncoded();
        }

        if (secret == null) {
            throw new IllegalStateException("Unable to determine key.");
        }

        if (authSecret != null) {
            secret = hkdfExpand(secret, authSecret, buildInfo("auth", new byte[0]), SHA_256_LENGTH);
        }

        return new byte[][]{
                secret,
                context
        };
    }

    public byte[][] deriveKeyAndNonce(byte[] salt, byte[] key, String keyId, ECPublicKey dh, byte[] authSecret, Encoding version, int mode) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] secret;
        byte[] keyInfo;
        byte[] nonceInfo;

        if (version == Encoding.AESGCM) {
            byte[][] secretAndContext = extractSecretAndContext(key, keyId, dh, authSecret);
            secret = secretAndContext[0];

            keyInfo = buildInfo("aesgcm", secretAndContext[1]);
            nonceInfo = buildInfo("nonce", secretAndContext[1]);
        } else if (version == Encoding.AES128GCM) {
            keyInfo = "Content-Encoding: aes128gcm\0".getBytes();
            nonceInfo = "Content-Encoding: nonce\0".getBytes();

            secret = extractSecret(key, keyId, dh, authSecret, mode);
        } else {
            throw new IllegalStateException("Unknown version: " + version);
        }

        byte[] hkdf_key = hkdfExpand(secret, salt, keyInfo, 16);
        byte[] hkdf_nonce = hkdfExpand(secret, salt, nonceInfo, 12);

        log("key", hkdf_key);
        log("nonce", hkdf_nonce);

        return new byte[][]{
                hkdf_key,
                hkdf_nonce
        };
    }

    private byte[] extractSecret(byte[] key, String keyId, ECPublicKey dh, byte[] authSecret, int mode) throws InvalidKeyException, NoSuchAlgorithmException {
        if (key != null) {
            if (key.length != KEY_LENGTH) {
                throw new IllegalArgumentException("An explicit key must be " + KEY_LENGTH + " bytes.");
            }
            return key;
        }

        if (dh == null) {
            KeyPair keyPair = keys.get(keyId);

            if (keyPair == null) {
                throw new IllegalArgumentException("No saved key for keyid '" + keyId + "'.");
            }

            return encode((ECPublicKey) keyPair.getPublic());
        }

        return webpushSecret(keyId, dh, authSecret, mode);
    }

    /**
     * Combine Shared and Authentication Secrets
     *
     * See https://tools.ietf.org/html/draft-ietf-webpush-encryption-09#section-3.3.
     *
     * @param keyId
     * @param dh
     * @param authSecret
     * @param mode
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public byte[] webpushSecret(String keyId, ECPublicKey dh, byte[] authSecret, int mode) throws NoSuchAlgorithmException, InvalidKeyException {
        ECPublicKey senderPubKey;
        ECPublicKey remotePubKey;
        ECPublicKey receiverPubKey;

        if (mode == ENCRYPT_MODE) {
            senderPubKey = getPublicKey(keyId);
            remotePubKey = dh;
            receiverPubKey = dh;
        } else if (mode == DECRYPT_MODE) {
            remotePubKey = getPublicKey(keyId);
            senderPubKey = remotePubKey;
            receiverPubKey = dh;
        } else {
            throw new IllegalArgumentException("Unsupported mode: " + mode);
        }

        log("remote pubkey", encode(remotePubKey));
        log("sender pubkey", encode(senderPubKey));
        log("receiver pubkey", encode(receiverPubKey));

        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(getPrivateKey(keyId));
        keyAgreement.doPhase(remotePubKey, true);
        byte[] secret = keyAgreement.generateSecret();

        byte[] ikm = secret;
        byte[] salt = authSecret;
        byte[] info = concat(WEB_PUSH_INFO.getBytes(), encode(receiverPubKey), encode(senderPubKey));

        return hkdfExpand(ikm, salt, info, SHA_256_LENGTH);
    }

    /**
     * Compute the shared secret (using the server's key pair and the client's public key) and the context.
     *
     * @param keyid
     * @param publicKey
     * @return
     */
    private  byte[][] extractDH(String keyid, ECPublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException {
        ECPublicKey senderPubKey = getPublicKey(keyid);

        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(getPrivateKey(keyid));
        keyAgreement.doPhase(publicKey, true);

        byte[] secret = keyAgreement.generateSecret();
        byte[] context = concat(labels.get(keyid).getBytes(UTF_8), new byte[1], lengthPrefix(publicKey), lengthPrefix(senderPubKey));

        return new byte[][]{
                secret,
                context
        };
    }

    /**
     * Get the public key for the given keyid.
     *
     * @param keyid
     * @return
     */
    private ECPublicKey getPublicKey(String keyid) {
        return (ECPublicKey) keys.get(keyid).getPublic();
    }

    /**
     * Get the private key for the given keyid.
     *
     * @param keyid
     * @return
     */
    private ECPrivateKey getPrivateKey(String keyid) {
        return (ECPrivateKey) keys.get(keyid).getPrivate();
    }


    /**
     * Encode the public key as a byte array and prepend its length in two bytes.
     *
     * @param publicKey
     * @return
     */
    private static byte[] lengthPrefix(ECPublicKey publicKey) {
        byte[] bytes = encode(publicKey);

        return concat(intToBytes(bytes.length), bytes);
    }

    /**
     * Convert an integer number to a two-byte binary number.
     *
     * This implementation:
     *   1. masks all but the lowest eight bits
     *   2. discards the lowest eight bits by moving all bits 8 places to the right.
     *
     * @param number
     * @return
     */
    private static byte[] intToBytes(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot convert a negative number, " + number + " given.");
        }

        if (number >= TWO_BYTE_MAX) {
            throw new IllegalArgumentException("Cannot convert an integer larger than " + (TWO_BYTE_MAX - 1) + " to two bytes.");
        }

        byte[] bytes = new byte[2];
        bytes[1] = (byte) (number & 0xff);
        bytes[0] = (byte) (number >> 8);

        return bytes;
    }

    /**
     * Print the length and unpadded url-safe base64 encoding of the byte array.
     *
     * @param info
     * @param array
     * @return
     */
    private static byte[] log(String info, byte[] array) {
        if ("1".equals(System.getenv("ECE_KEYLOG"))) {
            System.out.println(info + " [" + array.length + "]: " + Base64.getUrlEncoder().withoutPadding().encodeToString(array));
        }

        return array;
    }
}
