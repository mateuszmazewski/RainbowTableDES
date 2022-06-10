import keygenerators.IncrementalKeyGenerator;
import keygenerators.KeyGenerator;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RainbowTable {
    private final static int LOOKUP_TIMEOUT_SECS = 3600;

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

        this.modulo = getModulus();
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

    protected BigInteger getModulus() {
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
                fw.write(new String(entry.getKey().get())); // endKey
                fw.write(" ");
                fw.write(new String(entry.getValue().get())); // startKey
                fw.write("\n");
            }
            fw.close();
        } catch (IOException e) {
            System.err.println("Błąd podczas zapisywania do pliku: " + e.getMessage());
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
        String[] keys;
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
            nLines++;
            keys = line.split(" ");

            if (keys.length != 2) {
                throw new RuntimeException("Niepoprawny format danych, linia " + nLines);
            }

            if (keys[0].length() != DES.KEY_LENGTH || keys[1].length() != DES.KEY_LENGTH) {
                throw new RuntimeException("Błędna długość klucza w pliku, linia " + nLines);
            }

            endKey = keys[0].getBytes();
            startKey = keys[1].getBytes();

            table.put(new ByteArrayWrapper(endKey), new ByteArrayWrapper(startKey));
        }

        if (chainLength == null) {
            throw new RuntimeException("Nie udało się wczytać długości łańcucha z pliku");
        }

        return new RainbowTable(DES.KEY_LENGTH, chainLength, plaintext, table);
    }

    public byte[] lookup(String ciphertext) {
        return lookup(ciphertext, 1);
    }

    public byte[] lookup(String cryptogramToCrack, int threadCount) {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        DES[] deses = new DES[threadCount]; // Separate instance for each thread
        Runnable[] tasks = new Runnable[table.size()];
        int chainNumber = 0;
        AtomicInteger lookedChainsAtomic = new AtomicInteger();
        final byte[][] foundKey = new byte[1][]; // Array of byte[] because it needs to be final in order to access it from lambda expression
        foundKey[0] = null;

        for (int i = 0; i < threadCount; i++) {
            deses[i] = new DES();
        }

        for (ByteArrayWrapper startKey : table.values()) {
            tasks[chainNumber] = () -> {
                int threadId = (int) Thread.currentThread().getId() % threadCount;
                byte[] lookup = lookupChain(deses[threadId], startKey.get(), cryptogramToCrack);
                if (lookup != null) {
                    foundKey[0] = lookup;
                    System.out.println("Wątek " + Thread.currentThread().getId() % threadCount + " znalazł klucz. Zamykanie puli wątków.");
                    pool.shutdownNow();
                }

                lookedChainsAtomic.getAndIncrement();
            };
            chainNumber++;
        }

        ScheduledExecutorService progressExecutor = Executors.newSingleThreadScheduledExecutor();
        progressExecutor.scheduleAtFixedRate(() -> {
            double progressPercent = (double) lookedChainsAtomic.get() / (table.size()) * 100;
            System.out.println("Przeszukano: " + String.format("%.2f", progressPercent) + "%");
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        for (Runnable task : tasks) {
            pool.execute(task);
        }
        System.out.println("Liczba wątków przeszukujących tablicę: " + threadCount);
        pool.shutdown(); // Execute all initiated tasks and shutdown the pool

        try {
            boolean terminatedSuccessfully = pool.awaitTermination(LOOKUP_TIMEOUT_SECS, TimeUnit.SECONDS);
            if (!terminatedSuccessfully) {
                System.out.println("Przekroczono maksymalny czas przeszukiwania: " + LOOKUP_TIMEOUT_SECS + "s");
            }
        } catch (InterruptedException e) {
            System.err.println("Przerwano przeszukiwanie tablicy");
        }

        progressExecutor.shutdownNow();

        return foundKey[0];
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
