package org.tinystruct.system.security;

import org.tinystruct.AbstractApplication;
import org.tinystruct.system.annotation.Action;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyPairGenerator extends AbstractApplication {
    private final static Logger logger = Logger.getLogger(KeyPairGenerator.class.getName());

    @Override
    public void init() {
        this.setTemplateRequired(false);
    }

    @SuppressWarnings("Since11")
    @Action("generateKeyPair")
    public void getKeyPair() {
        String publicKeyFile = "/ca.pub", privateKeyFile = "/ca.pri";
        if (getContext().getAttribute("--public-key") != null) {
            publicKeyFile = getContext().getAttribute("--public-key").toString();
        }

        if (getContext().getAttribute("--private-key") != null) {
            privateKeyFile = getContext().getAttribute("--private-key").toString();
        }

        String password;
        if (getContext().getAttribute("--password") != null) {
            password = getContext().getAttribute("--password").toString();
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
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public String version() {
        return null;
    }
}
