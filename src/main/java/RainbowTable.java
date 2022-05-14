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
        String cipherText, endPass = startPass;

        try {
            for (int i = 0; i < chainLength; i++) {
                cipherText = des.encrypt(startPass);
                endPass = reduce(cipherText, i);
            }

            return endPass;
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String reduce(String cipherText, int position) {
        BigInteger index;
        StringBuilder sb = new StringBuilder();

        // Convert hex string into decimal value
        BigInteger temp = new BigInteger(cipherText, 16);
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

    public String lookup(String cipherTextToCrack) {
        String cipherText, endPass = null, lookup = null;
        long timeMillis = System.currentTimeMillis();

        // Start from the last reduction function
        for (int i = chainLength - 1; i >= 0; i--) {
            cipherText = cipherTextToCrack;

            try {
                for (int j = i; j < chainLength; j++) {
                    endPass = reduce(cipherText, j);
                    cipherText = des.encrypt(endPass);
                }
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }

            if (endPass != null && table.containsKey(endPass)) {
                lookup = lookupChain(table.get(endPass), cipherTextToCrack);
                if (lookup != null) {
                    break;
                }
            }
        }

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Lookup took " + seconds + "s");
        return lookup;
    }

    private String lookupChain(String startPass, String cipherTextToFind) {
        String cipherText;
        String password = startPass, lookup = null;

        try {
            for (int j = 0; j < chainLength; j++) {
                cipherText = des.encrypt(password);

                if (cipherText.equals(cipherTextToFind)) {
                    lookup = password;
                    break;
                }

                password = reduce(cipherText, j);
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return lookup;
    }

    public static void main(String[] args) {
        String charset = "abctajxyzne";
        int passwordLength = 5;
        int chainLength = 1000;
        int numChains = 10000;
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
        System.out.println("Table saved to file \"" + pathname + "\" in " + saveSeconds + "s\n");

        String cipherTextToCrack, foundPass;

        try {
            cipherTextToCrack = rainbowTable.des.encrypt("tajne");
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
