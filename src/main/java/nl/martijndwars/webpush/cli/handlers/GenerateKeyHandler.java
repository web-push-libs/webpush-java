package nl.martijndwars.webpush.cli.handlers;

import nl.martijndwars.webpush.Utils;
import nl.martijndwars.webpush.cli.commands.GenerateKeyCommand;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.*;
import java.util.Base64;

import static nl.martijndwars.webpush.Utils.ALGORITHM;
import static nl.martijndwars.webpush.Utils.CURVE;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public class GenerateKeyHandler implements HandlerInterface {
    private GenerateKeyCommand generateKeyCommand;

    public GenerateKeyHandler(GenerateKeyCommand generateKeyCommand) {
        this.generateKeyCommand = generateKeyCommand;
    }

    @Override
    public void run() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        KeyPair keyPair = generateKeyPair();

        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

        byte[] encodedPublicKey = Utils.encode(publicKey);
        byte[] encodedPrivateKey = Utils.encode(privateKey);

        if (generateKeyCommand.hasPublicKeyFile()) {
            writeKey(keyPair.getPublic(), new File(generateKeyCommand.getPublicKeyFile()));
        }

        System.out.println("PublicKey:");
        System.out.println(Base64.getUrlEncoder().encodeToString(encodedPublicKey));

        System.out.println("PrivateKey:");
        System.out.println(Base64.getUrlEncoder().encodeToString(encodedPrivateKey));
    }

    /**
     * Generate an EC keypair on the prime256v1 curve.
     *
     * @return
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     */
    public KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchProviderException, NoSuchAlgorithmException {
        ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec(CURVE);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER_NAME);
        keyPairGenerator.initialize(parameterSpec);

        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Write the given key to the given file.
     *
     * @param key
     * @param file
     */
    private void writeKey(Key key, File file) throws IOException {
        file.createNewFile();

        try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            PemObject pemObject = new PemObject("Key", key.getEncoded());

            pemWriter.writeObject(pemObject);
        }
    }
}
