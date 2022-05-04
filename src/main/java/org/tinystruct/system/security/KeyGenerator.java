package org.tinystruct.system.security;

import org.tinystruct.AbstractApplication;
import org.tinystruct.Application;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Resources;
import org.tinystruct.transfer.http.ReadableByteChannelWrapper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;

public class KeyGenerator extends AbstractApplication {
    private final static Logger logger = Logger.getLogger(KeyGenerator.class.getName());

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
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            SecureRandom secureRandom = new SecureRandom(password.getBytes());

            keyPairGenerator.initialize(1024, secureRandom);
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

    public static void main(String[] args) throws ApplicationException {
        ApplicationManager.install(new KeyGenerator());
        Context context = new ApplicationContext();
        context.setAttribute("--password", "0123");
        ApplicationManager.call("generateKeyPair", context);
    }
}
