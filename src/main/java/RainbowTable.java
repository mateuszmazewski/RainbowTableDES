import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class RainbowTable {
    private final char[] charset;
    private final int passwordLength;
    private final int chainLength;
    private final int numChains;
    private final BigInteger modulo;
    private Map<String, String> table; // <K, V> == <endPass, startPass>
    private final DES des;

    public RainbowTable(String charset, int passwordLength, int chainLength, int numChains) {
        this.charset = charset.toCharArray();
        this.passwordLength = passwordLength;
        this.chainLength = chainLength;
        this.numChains = numChains;
        modulo = getPrimeModulus();
        des = new DES();
    }

    public void generate() {
        table = new HashMap<>(numChains);
        String startPass, endPass;
        int collisions = 0;
        long timeMillis = System.currentTimeMillis();

        while (table.size() < numChains) {
            startPass = generateRandomPassword(passwordLength);

            // TODO: We shouldn't save duplicate chain but value lookup is very slow
            // If we change <K, V> to <startPass, endPass> then final lookup will slow down
            /*
            if(table.containsValue(startPass)) {
                // If startPass is already in the table -> whole chain would be a duplicate
                continue;
            }
            */

            endPass = generateChain(startPass);

            // If endPass is already in the table -> there was a collision in the chain
            // We cannot save it because map holds unique keys
            if (!table.containsKey(endPass)) {
                table.put(endPass, startPass);
            } else {
                collisions++;
            }
        }

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Table generated in " + seconds + "s" + " (" + collisions + " collisions)");
    }

    private String generateRandomPassword(int passwordLength) {
        StringBuilder sb = new StringBuilder(passwordLength);

        for (int i = 0; i < passwordLength; i++) {
            sb.append(charset[(int) (Math.random() * charset.length)]);
        }

        return sb.toString();
    }

    private String generateChain(String startPass) {
        byte[] cipherText;
        String endPass = startPass;

        try {
            for (int i = 0; i < chainLength; i++) {
                cipherText = des.encrypt(startPass.getBytes());
                endPass = reduce(cipherText, i);
            }

            return endPass;
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String reduce(byte[] cipherText, int position) {
        BigInteger index;
        StringBuilder sb = new StringBuilder();

        // Convert hex string into decimal value
        BigInteger temp = new BigInteger(DES.toHex(cipherText), 16);
        // Reduction output depends on the chain position
        temp = temp.add(BigInteger.valueOf(position));
        temp = temp.mod(modulo);

        while (temp.intValue() > 0) {
            index = temp.mod(BigInteger.valueOf(charset.length));
            sb.append(charset[index.intValue()]);
            temp = temp.divide(BigInteger.valueOf(charset.length));
        }

        return sb.toString();
    }

    private BigInteger getPrimeModulus() {
        BigInteger max = BigInteger.ZERO;

        for (int i = 1; i <= passwordLength; i++) {
            max = max.add(BigInteger.valueOf(charset.length).pow(i));
        }

        BigInteger prime = max.nextProbablePrime();
        System.out.println("prime modulus: " + prime);
        return prime;
    }

    public double saveTableToFile(String pathname) {
        if (table == null) {
            throw new NullPointerException("Table not generated");
        }

        File out = new File(pathname);
        FileWriter fw;
        long timeMillis = System.currentTimeMillis();

        try {
            fw = new FileWriter(out);
            for (Map.Entry<String, String> entry : table.entrySet()) {
                fw.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        timeMillis = System.currentTimeMillis() - timeMillis;
        return timeMillis / 1000.0;
    }

    public static void main(String[] args) {
        String charset = "0123456789abcdefghijklmnopqrstuvwxyz";
        int passwordLength = 5;
        int chainLength = 2;
        int numChains = 1000000;
        String pathname = "table.txt";

        RainbowTable rainbowTable = new RainbowTable(charset, passwordLength, chainLength, numChains);
        String sb = "Starting rainbow table generation:\n" +
                "password length: " + passwordLength + "\n" +
                "charset: " + charset + "\n" +
                "chains: " + numChains + "\n" +
                "reductions for each chain: " + chainLength + "\n";
        System.out.println(sb);

        rainbowTable.generate();
        double saveSeconds = rainbowTable.saveTableToFile(pathname);
        System.out.println("Table saved to file \"" + pathname + "\" in " + saveSeconds + "s");
    }
}
