import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class DES implements HashAlgorithm {
    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    private Cipher encryptor, decryptor;
    //private IvParameterSpec iv;

    public DES() {
        try {
            initialize(KeyGenerator.getInstance("DES").generateKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DES(SecretKeySpec secretKeySpec) {
        try {
            initialize(SecretKeyFactory.getInstance("DES").generateSecret(secretKeySpec));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialize(SecretKey secretKey) {
        try {
            encryptor = Cipher.getInstance("DES/ECB/PKCS5Padding");
            encryptor.init(Cipher.ENCRYPT_MODE, secretKey);
            //encryptor.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            decryptor = Cipher.getInstance("DES/ECB/PKCS5Padding");
            decryptor.init(Cipher.DECRYPT_MODE, secretKey);
            //decryptor.init(Cipher.DECRYPT_MODE, secretKey, iv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private IvParameterSpec generateIv() {
        byte[] iv = new byte[8];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public String encrypt(String plainText) throws BadPaddingException, IllegalBlockSizeException {
        return toHex(encryptor.doFinal(plainText.getBytes()));
    }

    public String decrypt(String cipherText) throws BadPaddingException, IllegalBlockSizeException {
        return toHex(decryptor.doFinal(hexStringToByteArray(cipherText)));
    }

    public static String toHex(byte[] data) {
        char[] hexChars = new char[data.length * 2];

        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    @Override
    public String hash(String text) throws BadPaddingException, IllegalBlockSizeException {
        return encrypt(text);
    }

}
