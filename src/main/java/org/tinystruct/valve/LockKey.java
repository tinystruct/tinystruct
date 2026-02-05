package org.tinystruct.valve;

import org.tinystruct.ApplicationRuntimeException;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Generate a random lock id.
 *
 * @author James Zhou
 */
public class LockKey {

    private final char[] alphabetNumbers = "0123456789-abcdefghijklmnopqrstuvwxyz".toCharArray();
    private final byte[] fixedBytes = new byte[36];
    private final byte[] idb;

    public LockKey(byte[] idb) {
        this.idb = idb;
    }

    public String value() {
        for (int i = 0; i < fixedBytes.length; i++) {
            if (i < idb.length)
                fixedBytes[i] = idb[i];
            else {
                try {
                    fixedBytes[i] = (byte) (alphabetNumbers[SecureRandom.getInstance("SHA1PRNG")
                            .nextInt(alphabetNumbers.length - 1)]);
                } catch (NoSuchAlgorithmException e) {
                    throw new ApplicationRuntimeException(e);
                }
            }
        }

        return new String(fixedBytes, Charset.defaultCharset());
    }
}
