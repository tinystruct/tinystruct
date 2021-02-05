package org.tinystruct.valve;

import java.util.Random;

/**
 * Generate a random lock id.
 *
 * @author James Zhou
 */
public class LockKey {

    private char[] alphabetNumbers = "0123456789-abcdefghijklmnopqrstuvwxyz".toCharArray();
    private byte[] fixedBytes = new byte[36];
    private final byte[] idb;

    public LockKey(byte[] idb) {
        this.idb = idb;
    }

    public String value() {
        for (int i = 0; i < fixedBytes.length; i++) {
            if (i < idb.length)
                fixedBytes[i] = idb[i];
            else
                fixedBytes[i] = (byte) (alphabetNumbers[new Random().nextInt(alphabetNumbers.length - 1)]);
        }

        return new String(fixedBytes);
    }
}
