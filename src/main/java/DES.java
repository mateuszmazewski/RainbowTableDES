import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.Random;

public class DES {
    public static final int KEY_LENGTH = 8;
    private static final String ALG_MODE_PADDING = "DES/ECB/PKCS5Padding";
    public static final String DES_KEY_CHARSET = "0123456789";

    private Cipher encryptor, decryptor;

    public DES() {
        try {
            encryptor = Cipher.getInstance(ALG_MODE_PADDING);
            decryptor = Cipher.getInstance(ALG_MODE_PADDING);
        } catch (Exception e) {
            System.err.println("Błąd podczas inicjowania algorytmu DES");
        }
    }

    public void initializeEncryptor(String key) {
        validateKey(key);

        try {
            encryptor.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(), "DES"));
        } catch (Exception e) {
            System.err.println("Błąd podczas inicjowania szyfratora kluczem");
        }
    }

    public void initializeEncryptor(byte[] key) {
        validateKey(key);

        try {
            encryptor.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
        } catch (Exception e) {
            System.err.println("Błąd podczas inicjowania szyfratora kluczem");
        }
    }

    public void initializeDecryptor(String key) {
        validateKey(key);

        try {
            decryptor.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "DES"));
        } catch (Exception e) {
            System.err.println("Błąd podczas inicjowania deszyfratora kluczem");
        }
    }

    public void initializeDecryptor(byte[] key) {
        validateKey(key);

        try {
            decryptor.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DES"));
        } catch (Exception e) {
            System.err.println("Błąd podczas inicjowania deszyfratora kluczem");
        }
    }

    private void validateKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Klucz nie może być pusty");
        }
        if (!key.matches("^\\d{" + KEY_LENGTH + "}$")) {
            throw new IllegalArgumentException("Klucz musi mieć długość dokładnie " + KEY_LENGTH + " bajtów (składać się z " + KEY_LENGTH + " cyfr). Aktualny klucz: " + key);
        }
    }

    private void validateKey(byte[] key) {
        if (key == null) {
            throw new IllegalArgumentException("Klucz nie może być pusty");
        }
        if (key.length != KEY_LENGTH) {
            throw new IllegalArgumentException("Klucz musi mieć długość " + KEY_LENGTH + " bajtów");
        }

        for (byte b : key) {
            if (b < 48 || b > 57) {
                throw new IllegalArgumentException("Klucz musi składać się z samych cyfr. Aktualny klucz: " + new String(key));
            }
        }
    }

    public String encrypt(String plainText) {
        String hexCipherText = null;

        try {
            hexCipherText = Hex.toHex(encryptor.doFinal(plainText.getBytes()));
        } catch (Exception e) {
            System.err.println("Nie udało się zaszyfrować");
        }

        return hexCipherText;
    }

    public String decrypt(String cipherText) {
        String hexPlainText = null;

        try {
            hexPlainText = Hex.toHex(decryptor.doFinal(Hex.hexStringToByteArray(cipherText)));
        } catch (Exception e) {
            System.err.println("Nie udało się zdeszyfrować");
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
