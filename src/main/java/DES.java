import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;

public class DES {
    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    private Cipher encryptor, decryptor;
    //private IvParameterSpec iv;

    public DES() {
        try {
            SecretKey secretKey = KeyGenerator.getInstance("DES").generateKey();
            //iv = generateIv();

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

    public byte[] encrypt(byte[] plainText) throws BadPaddingException, IllegalBlockSizeException {
        return encryptor.doFinal(plainText);
    }

    public byte[] decrypt(byte[] cipherText) throws BadPaddingException, IllegalBlockSizeException {
        return decryptor.doFinal(cipherText);
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
}
