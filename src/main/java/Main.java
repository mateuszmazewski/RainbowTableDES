import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class Main {

    public static void main(String[] args) {
        String charset = "abctajxyzne";
        int passwordLength = 5;
        int chainLength = 1000;
        int numChains = 140; // set by trial and error, higher values may cause infinite(?) generation
        String pathname = "table.txt";
        DES des = new DES();

        RainbowTable rainbowTable = new RainbowTableVerbose(charset, passwordLength, chainLength, des);
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

        String cipherTextToCrack, foundPass;

        try {
            cipherTextToCrack = des.encrypt("tajne");
            foundPass = rainbowTable.lookup(cipherTextToCrack);
            if (foundPass != null) {
                System.out.println("For cipherText: " + cipherTextToCrack + " found password: " + foundPass);
            } else {
                System.out.println("Rainbow table doesn't contain the password for given cipherText: " + cipherTextToCrack);
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }
}
