/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCSException;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Key store builder.
 */
@SuppressWarnings({"checkstyle:ClassFanOutComplexity"})
public class KeyStoreBuilder {
    private KeyStoreBuilder() {
    }

    /**
     * Build a key store file in PKCS12 format.
     *
     * @param crtPath Input public certificate file
     * @param keyPath Input private key file
     * @param keyStorePath Output path for the key store file
     * @param password Key store password
     *                     
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static void build(String crtPath, String keyPath, String keyStorePath, String password) throws GeneralSecurityException, IOException {
        try (FileOutputStream stream = new FileOutputStream(keyStorePath)) {
            KeyStore keyStore = KeyStoreBuilder.createKeyStore(crtPath, keyPath, password);
            byte[] bytes = KeyStoreBuilder.convertKeyStoreToBytes(keyStore, password);
            stream.write(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Keystore build failed", e);
        }
    }

    private static KeyStore createKeyStore(String crtPath, String keyPath, String password) throws IOException, GeneralSecurityException, PKCSException {
        KeyStore keyStore = createEmptyKeyStore();
        X509Certificate publicCert = loadCertificate(crtPath);
        PrivateKey privateKey = loadPrivateKey(keyPath);
        keyStore.setCertificateEntry("publicCrtAlias", publicCert);
        keyStore.setKeyEntry("privateKeyAlias", privateKey, password.toCharArray(), new Certificate[] {publicCert});
        return keyStore;
    }

    private static KeyStore createEmptyKeyStore() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        return keyStore;
    }

    private static X509Certificate loadCertificate(String path) throws GeneralSecurityException, IOException {
        Security.addProvider(new BouncyCastleProvider());
        PEMParser pemParser = new PEMParser(new FileReader(path));
        JcaX509CertificateConverter x509Converter = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider());
        X509Certificate publicCrt = x509Converter.getCertificate((X509CertificateHolder) pemParser.readObject());
        return publicCrt;
    }

    private static PrivateKey loadPrivateKey(String path) throws IOException, PKCSException {
        try (FileReader keyReader = new FileReader(path)) {
            PEMParser pemParser = new PEMParser(keyReader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object object = pemParser.readObject();
            if (object instanceof PrivateKeyInfo) {
                PrivateKeyInfo pki = PrivateKeyInfo.getInstance(object);
                return converter.getPrivateKey(pki);
            } else if (object instanceof PEMKeyPair) {
                KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
                return kp.getPrivate();
            } else {
                throw new PKCSException("Invalid private key class: " + object.getClass().getName());
            }
        }
    }
    
    private static byte[] convertKeyStoreToBytes(KeyStore keyStore, String password) throws IOException, GeneralSecurityException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        keyStore.store(out, password.toCharArray());
        byte[] bytes = out.toByteArray();
        return bytes;
    }
}
