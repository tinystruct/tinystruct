package org.tinystruct.system.security;

import org.tinystruct.AbstractApplication;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.logging.Logger;

public class KeyPairGenerator extends AbstractApplication {
    private final static Logger logger = Logger.getLogger(KeyPairGenerator.class.getName());

    @Override
    public void init() {
        this.setAction("generateKeyPair", "getKeyPair");
        this.setTemplateRequired(false);
    }

    @SuppressWarnings("Since11")
    public void getKeyPair() {
        String publicKeyFile = "/ca.pub", privateKeyFile = "/ca.pri";
        if (context.getAttribute("--public-key") != null) {
            publicKeyFile = context.getAttribute("--public-key").toString();
        }

        if (context.getAttribute("--private-key") != null) {
            privateKeyFile = context.getAttribute("--private-key").toString();
        }

        String password;
        if (context.getAttribute("--password") != null) {
            password = context.getAttribute("--password").toString();
        } else {
            logger.info("Password is required. please use --password PASSWORD option.");
            return;
        }

        try {
            java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA");
            SecureRandom secureRandom = new SecureRandom(password.getBytes());

            keyPairGenerator.initialize(2048, secureRandom);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            byte[] privateKey = keyPair.getPrivate().getEncoded();
            byte[] publicKey = keyPair.getPublic().getEncoded();

            Files.writeString(Paths.get("src", "main", "resources", publicKeyFile), new String(publicKey));
            Files.writeString(Paths.get("src", "main", "resources", privateKeyFile), new String(privateKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String version() {
        return null;
    }
}
