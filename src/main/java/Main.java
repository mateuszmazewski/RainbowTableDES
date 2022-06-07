import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        String charset = "abctajxyzne";
        int passwordLength = 8;
        int chainLength = 1000;
        int numChains = 140; // set by trial and error, higher values may cause infinite(?) generation
        String pathname = "table.txt";

        byte[] secretKey = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        DES des = new DES(new SecretKeySpec(secretKey, "DES"));

        RainbowTable rainbowTable = new RainbowTableVerbose(passwordLength, chainLength, "tajne");
        String s = "Starting rainbow table generation:\n" +
                "password length: " + passwordLength + "\n" +
                "charset: " + charset + "\n" +
                "chains: " + numChains + "\n" +
                "reductions for each chain: " + chainLength + "\n";
        System.out.println(s);

        try {
            rainbowTable.generate(numChains, 4);
        } catch (InterruptedException ignored) {}
        double saveSeconds = rainbowTable.saveTableToFile(pathname);
        System.out.println("Table saved to file \"" + pathname + "\" in " + saveSeconds + "s\n");

        String cipherTextToCrack;
        byte[] foundKey;

        try {
            cipherTextToCrack = des.encrypt("tajne");
            foundKey = rainbowTable.lookup(cipherTextToCrack);
            if (foundKey != null) {
                System.out.println("For cipherText: " + cipherTextToCrack + " found key: " + Arrays.toString(foundKey));
            } else {
                System.out.println("Rainbow table doesn't contain the password for given cipherText: " + cipherTextToCrack);
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }
}
