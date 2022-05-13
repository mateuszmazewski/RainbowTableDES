import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class RainbowTable {

    public static void main(String[] args) {
        String s = "Tajne";
        DES des = new DES();

        try {
            byte[] cipherText = des.encrypt(s.getBytes());
            System.out.println(DES.toHex(cipherText));
            byte[] plainText = des.decrypt(cipherText);
            System.out.println(new String(plainText));
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }
}
