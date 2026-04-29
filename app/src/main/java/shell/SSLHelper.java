package shell;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SSLHelper {

    static {
        // Register the provider for any internal lookups
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static SSLServerSocketFactory createSSLContext() throws Exception {
        // 1. Instantiate the provider directly to bypass String-name lookups in Native Image
        BouncyCastleProvider bcProvider = new BouncyCastleProvider();

        // 2. Generate RSA KeyPair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // 3. Define Certificate attributes
        X500Name commonName = new X500Name("CN=AttackerC2");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24); // Yesterday
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365); // 1 year

        // 4. Build the Certificate
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                commonName, serial, notBefore, notAfter, commonName, keyPair.getPublic());

        // FIX: Use the bcProvider OBJECT directly
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(bcProvider)
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);

        // FIX: Use the bcProvider OBJECT directly
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(bcProvider)
                .getCertificate(certHolder);

        // 5. Create in-memory KeyStore (Standard Java JCE works fine in Native)
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] ksPassword = "password".toCharArray();
        ks.load(null, ksPassword);
        ks.setKeyEntry("attacker-key", keyPair.getPrivate(), ksPassword, new X509Certificate[]{cert});

        // 6. Initialize KeyManager with our KeyStore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, ksPassword);

        // 7. Create and initialize SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext.getServerSocketFactory();
    }
}
