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
    private final Map<String, String> table; // <K, V> == <endPass, startPass>
    private final HashAlgorithm hashAlgorithm;
    private final Object addLock;
    private int generatedChains;

    public RainbowTable(String charset, int passwordLength, int chainLength, int numChains, HashAlgorithm hashAlgorithm) {
        this.charset = charset.toCharArray();
        this.passwordLength = passwordLength;
        this.chainLength = chainLength;
        this.numChains = numChains;
        this.modulo = getPrimeModulus();
        this.table = new ConcurrentHashMap<>(numChains);
        this.hashAlgorithm = hashAlgorithm;
        this.addLock = new Object();
    }

    protected void generationThread(int count, int threadId, int threadCount) {
        PasswordGenerator passwordGenerator = new IncrementalPasswordGenerator(charset, threadId + 1);
        String startPass, endPass;
        boolean done = false;

        while (!done) {
            startPass = passwordGenerator.next(threadCount);
            endPass = generateChain(startPass);

            // If endPass is already in the table -> there was a collision in the chain
            // We cannot save it because map holds unique keys
            synchronized (addLock) {
                if (generatedChains < numChains) {
                    if (!table.containsKey(endPass)) {
                        table.put(endPass, startPass);
                        generatedChains++;
                    }
                }
                if (generatedChains >= numChains) {
                    done = true;
                }
            }
        }
    }

    public void generate(int threadCount) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];

        generatedChains = 0;

        for(int i = 0; i < threadCount; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> generationThread(numChains / threadCount, finalI, threadCount));
            threads[i].start();
        }

        for(Thread t : threads) {
            t.join();
        }
    }

    public void generate() {
        generationThread(numChains, 0, 1);
    }

    private String generateChain(String startPass) {
        String cipherText, endPass = startPass;

        try {
            for (int i = 0; i < chainLength; i++) {
                cipherText = hashAlgorithm.hash(endPass);
                endPass = reduce(cipherText, i);
            }

            return endPass;
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String reduce(String cipherText, int position) {
        // Convert hex string into decimal value
        BigInteger temp = new BigInteger(cipherText, 16);
        // Reduction output depends on the chain position
        temp = temp.add(BigInteger.valueOf(position));
        temp = temp.mod(modulo);

        return new IncrementalPasswordGenerator(charset, temp.intValue() + 1).next(0);
    }

    protected BigInteger getPrimeModulus() {
        return BigInteger.valueOf(charset.length).pow(passwordLength);
    }

    public double saveTableToFile(String pathname) {
        if (table == null || table.size() == 0) {
            throw new IllegalStateException("Table not generated");
        }

        File out = new File(pathname);
        FileWriter fw;
        long timeMillis = System.currentTimeMillis();

        try {
            fw = new FileWriter(out);
            for (Map.Entry<String, String> entry : table.entrySet()) {
                fw.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        timeMillis = System.currentTimeMillis() - timeMillis;
        return timeMillis / 1000.0;
    }

    public String lookup(String cipherTextToCrack) {
        String cipherText, endPass = null, lookup = null;

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
