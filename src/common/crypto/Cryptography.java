package common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public abstract class Cryptography {

    private static final SecureRandom randomGenerator = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();
    private static final byte[] HEX_ALPHABET = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    private static final int TOKEN_LEN = 16;
    private static final String HASH_ALG = "MD5";
    
    private Cryptography() {}


    public static String getSecureToken() {
        byte[] newToken = new byte[TOKEN_LEN];
        randomGenerator.nextBytes(newToken);
        return base64Encoder.encodeToString(newToken);
    }

    public static String digest(String message) throws NoSuchAlgorithmException {
        return bytesToHex(MessageDigest.getInstance(HASH_ALG).digest(message.getBytes()));
    }

    // @author: maybewecouldstealavan on StackOverflow
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ALPHABET[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ALPHABET[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

}