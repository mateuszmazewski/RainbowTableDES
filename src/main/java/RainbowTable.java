import keygenerators.IncrementalKeyGenerator;
import keygenerators.KeyGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RainbowTable {
    private final byte[] byteset;
    private final int passwordLength;
    private final int chainLength;
    private final String plaintext;
    private final BigInteger modulo;
    private Map<byte[], byte[]> table; // <K, V> == <endKey, startKey>
    private final Object addLock;
    private int generatedChains;

    // TODO: remove passwordLength and chainLength?
    public RainbowTable(int passwordLength, int chainLength, String plaintext) {
        this.byteset = new byte[10];
        for (int i = 0; i < byteset.length; i++) {
            byteset[i] = (byte) (i + 48);
        }

        this.passwordLength = passwordLength;
        this.chainLength = chainLength;
        this.plaintext = plaintext;

        this.modulo = getPrimeModulus();
        this.addLock = new Object();
    }

    protected void generationThread(int numChains, int threadId, int threadCount) {
        DES des = new DES(); // thread's private DES instance -- in order not to mix keys in one shared DES instance
        KeyGenerator keyGenerator = new IncrementalKeyGenerator(byteset, threadId);
        byte[] startKey, endKey;
        boolean done = false;

        while (!done) {
            startKey = keyGenerator.next((long) threadCount);
            endKey = generateChain(des, startKey);

            // If endKey is already in the table -> there was a collision in the chain
            // We cannot save it because map holds unique keys
            synchronized (addLock) {
                if (generatedChains < numChains) {
                    if (!table.containsKey(endKey)) {
                        table.put(endKey, startKey);
                        generatedChains++;
                    }
                }
                if (generatedChains >= numChains) {
                    done = true;
                }
            }
        }
    }

    public void generate(int numChains, int threadCount) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];

        table = new ConcurrentHashMap<>(numChains);
        generatedChains = 0;

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> generationThread(numChains, finalI, threadCount));
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }
    }

    public void generate(int numChains) {
        generationThread(numChains, 0, 1);
    }

    private byte[] generateChain(DES des, byte[] startKey) {
        String cryptogram;
        byte[] endKey = startKey;

        for (int i = 0; i < chainLength; i++) {
            des.initializeEncryptor(endKey);
            cryptogram = des.encrypt(plaintext);
            endKey = reduce(cryptogram, i);
        }

        return endKey;
    }

    private byte[] reduce(String cryptogram, int position) {
        // Convert hex string into decimal value
        BigInteger temp = new BigInteger(cryptogram, 16);
        // Reduction output depends on the chain position
        temp = temp.add(BigInteger.valueOf(position));
        temp = temp.mod(modulo);

        return new IncrementalKeyGenerator(byteset, temp).next(0L);
    }

    protected BigInteger getPrimeModulus() {
        return BigInteger.valueOf(byteset.length).pow(passwordLength);
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
            for (Map.Entry<byte[], byte[]> entry : table.entrySet()) {
                fw.write(Arrays.toString(entry.getKey())); // endKey
                fw.write(Arrays.toString(entry.getValue())); // startKey
                fw.write("\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        timeMillis = System.currentTimeMillis() - timeMillis;
        return timeMillis / 1000.0;
    }

    public byte[] lookup(DES des, String cryptogramToCrack) {
        String cryptogram;
        byte[] endKey = null, lookup = null;

        // Start from the last reduction function
        for (int i = chainLength - 1; i >= 0; i--) {
            cryptogram = cryptogramToCrack;

            for (int j = i; j < chainLength; j++) {
                endKey = reduce(cryptogram, j);
                des.initializeEncryptor(endKey);
                cryptogram = des.encrypt(plaintext);
            }

            if (endKey != null && table.containsKey(endKey)) {
                lookup = lookupChain(des, table.get(endKey), cryptogramToCrack);
                if (lookup != null) {
                    break;
                }
            }
        }

        return lookup;
    }

    private byte[] lookupChain(DES des, byte[] startKey, String cryptogramToFind) {
        String cryptogram;
        byte[] key = startKey, lookup = null;

        for (int j = 0; j < chainLength; j++) {
            des.initializeEncryptor(key);
            cryptogram = des.encrypt(plaintext);

            if (cryptogram.equals(cryptogramToFind)) {
                lookup = key;
                break;
            }

            key = reduce(cryptogram, j);
        }

        return lookup;
    }

}
