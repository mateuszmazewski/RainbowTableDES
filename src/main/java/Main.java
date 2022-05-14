import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class Main {

    public static void main(String[] args) {
        String charset = "abctajxyzne";
        int passwordLength = 5;
        int chainLength = 1000;
        int numChains = 10000;
        String pathname = "table.txt";
        HashAlgorithm des = new DES();

        RainbowTable rainbowTable = new RainbowTable(charset, passwordLength, chainLength, numChains, des);
        String sb = "Starting rainbow table generation:\n" +
                "password length: " + passwordLength + "\n" +
                "charset: " + charset + "\n" +
                "chains: " + numChains + "\n" +
                "reductions for each chain: " + chainLength + "\n";
        System.out.println(sb);

        try {
            rainbowTable.generate(4);
        } catch (InterruptedException ignored) {}
        double saveSeconds = rainbowTable.saveTableToFile(pathname);
        System.out.println("Table saved to file \"" + pathname + "\" in " + saveSeconds + "s\n");

        String cipherTextToCrack, foundPass;

        try {
            cipherTextToCrack = des.hash("tajne");
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
