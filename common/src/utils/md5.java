package utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class md5 {
    public static String fromByteArray(byte[] input) throws NoSuchAlgorithmException {
        String result;
        MessageDigest md = md5.getInstance();
        md.update(input, 0, input.length);
        result = new BigInteger(1, md.digest()).toString(16);
        return result;
    }

    public static MessageDigest getInstance() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5");
    }
}
