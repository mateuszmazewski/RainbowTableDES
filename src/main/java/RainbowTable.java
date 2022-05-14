import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RainbowTable {
    private final char[] charset;
    private final int passwordLength;
    private final int chainLength;
    private final int numChains;
    private final BigInteger modulo;
    private Map<String, String> table; // <K, V> == <endPass, startPass>
    private final HashAlgorithm hashAlgorithm;

    public RainbowTable(String charset, int passwordLength, int chainLength, int numChains, HashAlgorithm hashAlgorithm) {
        this.charset = charset.toCharArray();
        this.passwordLength = passwordLength;
        this.chainLength = chainLength;
        this.numChains = numChains;
        this.modulo = getPrimeModulus();
        this.table = new ConcurrentHashMap<>(numChains); // TODO: use separate table for each thread and join afterwards?
        this.hashAlgorithm = hashAlgorithm;
    }

    public void generate(int count) {
        String startPass, endPass;
        int collisions = 0;
        long timeMillis = System.currentTimeMillis();
        int generatedChains = 0;

        while (generatedChains < count) {
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
                generatedChains++;
            } else {
                collisions++;
            }
        }

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Table generated in " + seconds + "s" + " (" + collisions + " collisions)");
    }

    public void generate(int count, int threadCount) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];

        for(int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                // TODO: include remainder
                generate(count / threadCount);
            });
            threads[i].start();
        }

        for(Thread t : threads) {
            t.join();
        }
    }

    // TODO: randomness yields inconsistent results
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
                cipherText = hashAlgorithm.hash(startPass);
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
        // max is calculated as geometric progression sum (a1=q=charset.length, n=passwordLength)
        BigInteger charsetLength = BigInteger.valueOf(charset.length);
        BigInteger numerator = charsetLength.multiply(BigInteger.ONE.subtract(charsetLength.pow(passwordLength)));
        BigInteger denominator = BigInteger.ONE.subtract(charsetLength);

        BigInteger max = numerator.divide(denominator);
        BigInteger prime = max.nextProbablePrime();
        System.out.println("prime modulus: " + prime);
        return prime;
    }

    public double saveTableToFile(String pathname) {
        if (table == null || table.size() == 0) {
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
                    cipherText = hashAlgorithm.hash(endPass);
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
                cipherText = hashAlgorithm.hash(password);

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

}
