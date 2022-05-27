import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.Random;

public class DES {
    private static final int KEY_LENGTH = 8;
    private static final String ALG_MODE_PADDING = "DES/ECB/PKCS5Padding";
    public static final String DES_KEY_CHARSET = "0123456789";

    private Cipher encryptor, decryptor;

    public DES() {
        try {
            encryptor = Cipher.getInstance(ALG_MODE_PADDING);
            decryptor = Cipher.getInstance(ALG_MODE_PADDING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initializeEncryptor(String key) {
        validateKey(key);

        try {
            encryptor.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(), "DES"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initializeDecryptor(String key) {
        validateKey(key);

        try {
            decryptor.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "DES"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validateKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Klucz nie może być pusty");
        }
        if (!key.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("Klucz musi mieć długość dokładnie 8 bajtów (składać się z 8 cyfr)");
        }
    }

    public String encrypt(String plainText) {
        String hexCipherText = null;

        try {
            hexCipherText = Hex.toHex(encryptor.doFinal(plainText.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hexCipherText;
    }

    public String decrypt(String cipherText) {
        String hexPlainText = null;

        try {
            hexPlainText = Hex.toHex(decryptor.doFinal(Hex.hexStringToByteArray(cipherText)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hexPlainText;
    }

    public static String generateRandomKey() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < KEY_LENGTH; i++)
            sb.append(DES_KEY_CHARSET.charAt(rand.nextInt(DES_KEY_CHARSET.length())));
        return sb.toString();
    }

}
