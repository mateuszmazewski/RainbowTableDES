import keygenerators.IncrementalKeyGenerator;
import keygenerators.KeyGenerator;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RainbowTable {
    private final byte[] byteset;
    private final int passwordLength;
    private final int chainLength;
    private final String plaintext;
    private final BigInteger modulo;
    private Map<ByteArrayWrapper, ByteArrayWrapper> table; // <K, V> == <endKey, startKey>
    private final Object addLock;

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

    protected RainbowTable(int passwordLength, int chainLength, String plaintext, Map<ByteArrayWrapper, ByteArrayWrapper> table) {
        this(passwordLength, chainLength, plaintext);
        this.table = table;
    }

    protected void generationThread(int numChains, int threadId, int threadCount) {
        DES des = new DES(); // thread's private DES instance -- in order not to mix keys in one shared DES instance
        KeyGenerator keyGenerator = new IncrementalKeyGenerator(byteset, threadId);
        byte[] startKey, endKey;
        boolean done = false;

        while (!done) {
            startKey = keyGenerator.next((long) threadCount);
            endKey = generateChain(des, startKey);

            synchronized (addLock) {
                if (table.size() < numChains) {
                    table.put(new ByteArrayWrapper(endKey), new ByteArrayWrapper(startKey));
                }
                if (table.size() >= numChains) {
                    done = true;
                }
            }
        }
    }

    public void generate(int numChains, int threadCount) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];

        table = new ConcurrentHashMap<>(numChains);

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> generationThread(numChains, finalI, threadCount));
            threads[i].start();
        }

        ScheduledExecutorService progressExecutor = Executors.newSingleThreadScheduledExecutor();
        progressExecutor.scheduleAtFixedRate(() -> {
            double progressPercent = (double) table.size() / (numChains) * 100;
            System.out.println("Postęp generowania: " + String.format("%.2f", progressPercent) + "%");
        }, 10000, 10000, TimeUnit.MILLISECONDS);

        for (Thread t : threads) {
            t.join();
        }

        progressExecutor.shutdownNow();
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

    public void saveToFile(String pathname) {
        if (table == null || table.size() == 0) {
            throw new IllegalStateException("Table not generated");
        }

        File out = new File(pathname);
        FileWriter fw;

        try {
            fw = new FileWriter(out);

            fw.write("chainLength=" + chainLength + "\n");
            fw.write("plaintext=" + plaintext + "\n");
            for (Map.Entry<ByteArrayWrapper, ByteArrayWrapper> entry : table.entrySet()) {
                fw.write(Arrays.toString(entry.getKey().get())); // endKey
                fw.write("#");
                fw.write(Arrays.toString(entry.getValue().get())); // startKey
                fw.write("\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RainbowTable readFromFile(String pathname) throws IOException {
        // NOTE: not thread-safe, write with one thread only
        Map<ByteArrayWrapper, ByteArrayWrapper> table = new HashMap<>();
        BufferedReader reader;
        int nLines = 0;
        Integer chainLength = null;
        String plaintext;

        reader = new BufferedReader(new FileReader(pathname));

        String line;
        String[] arrays;
        String[] splittedArray;
        byte[] endKey;
        byte[] startKey;

        line = reader.readLine();
        nLines++;
        if (line != null && line.startsWith("chainLength=")) {
            line = line.replaceFirst("^chainLength=", "");
            chainLength = Integer.parseInt(line);
        }

        line = reader.readLine();
        nLines++;
        if (line != null && line.startsWith("plaintext=")) {
            plaintext = line.replaceFirst("^plaintext=", "");
        } else {
            throw new RuntimeException("Nie udało się wczytać tekstu jawnego z pliku");
        }

        while ((line = reader.readLine()) != null) {
            endKey = new byte[8];
            startKey = new byte[8];
            nLines++;
            arrays = line.split("#");

            if (arrays.length != 2) {
                throw new RuntimeException("Niepoprawny format danych, linia " + nLines);
            }

            arrays[0] = arrays[0].replace("[", "").replace("]", "");
            arrays[1] = arrays[1].replace("[", "").replace("]", "");

            splittedArray = arrays[0].split(", ");
            if (splittedArray.length != 8) {
                throw new RuntimeException("Błędna długość klucza w pliku, linia " + nLines);
            }
            for (int i = 0; i < 8; i++) {
                endKey[i] = Byte.parseByte(splittedArray[i]);
            }
            splittedArray = arrays[1].split(", ");
            if (splittedArray.length != 8) {
                throw new RuntimeException("Błędna długość klucza w pliku, linia " + nLines);
            }
            for (int i = 0; i < 8; i++) {
                startKey[i] = Byte.parseByte(splittedArray[i]);
            }

            table.put(new ByteArrayWrapper(endKey), new ByteArrayWrapper(startKey));
        }

        if (chainLength == null) {
            throw new RuntimeException("Nie udało się wczytać długości łańcucha z pliku");
        }

        return new RainbowTable(8, chainLength, plaintext, table);
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

            ByteArrayWrapper endKeyWrapped = new ByteArrayWrapper(endKey);
            if (endKey != null && table.containsKey(endKeyWrapped)) {
                lookup = lookupChain(des, table.get(endKeyWrapped).get(), cryptogramToCrack);
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

    public int getTableSize() {
        return table.size();
    }

    public String getPlaintext() {
        return plaintext;
    }

    public int getChainLength() {
        return chainLength;
    }

    public int getPasswordLength() {
        return passwordLength;
    }

    protected Map<ByteArrayWrapper, ByteArrayWrapper> getTable() {
        return table;
    }

}
