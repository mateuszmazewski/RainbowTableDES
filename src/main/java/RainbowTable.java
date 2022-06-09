import keygenerators.IncrementalKeyGenerator;
import keygenerators.KeyGenerator;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RainbowTable {
    private final byte[] byteset;
    private final int passwordLength;
    private int chainLength;
    private String plaintext;
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

        ScheduledExecutorService progressExecutor = Executors.newSingleThreadScheduledExecutor();
        progressExecutor.scheduleAtFixedRate(() -> {
            double progressPercent = (double) generatedChains / (numChains) * 100;
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

    public double saveTableToFile(String pathname) {
        if (table == null || table.size() == 0) {
            throw new IllegalStateException("Table not generated");
        }

        File out = new File(pathname);
        FileWriter fw;
        long timeMillis = System.currentTimeMillis();

        try {
            fw = new FileWriter(out);

            fw.write("chainLength=" + chainLength + "\n");
            fw.write("plaintext=" + plaintext + "\n");
            for (Map.Entry<byte[], byte[]> entry : table.entrySet()) {
                fw.write(Arrays.toString(entry.getKey())); // endKey
                fw.write("#");
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

    public boolean readTableFromFile(String pathname) {
        table = new ConcurrentHashMap<>();
        BufferedReader reader;
        int nLines = 0;

        try {
            reader = new BufferedReader(new FileReader(pathname));
        } catch (FileNotFoundException e) {
            System.err.println("Plik nie istnieje: \"" + pathname + "\"");
            return false;
        }

        String line;
        String[] arrays;
        String[] splittedArray;
        byte[] endKey;
        byte[] startKey;

        try {
            line = reader.readLine();
            nLines++;
            if (line != null && line.startsWith("chainLength")) {
                line = line.replace("chainLength=", "");
                try {
                    chainLength = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    System.err.println("Błędny format długości łańcucha w pliku");
                    return false;
                }
            }

            line = reader.readLine();
            nLines++;
            if (line != null && line.startsWith("plaintext")) {
                plaintext = line.replace("plaintext=", "");
            } else {
                System.err.println("Nie udało się wczytać tekstu jawnego z pliku");
                return false;
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

                table.put(endKey, startKey);
            }
        } catch (IOException ioe) {
            System.err.println("Błąd podczas wczytywania pliku, linia " + nLines + ": " + ioe.getMessage());
            return false;
        } catch (NumberFormatException nfe) {
            System.err.println("Błąd podczas przetwarzania danych z pliku, linia " + nLines + ": " + nfe.getMessage());
            return false;
        } catch (RuntimeException re) {
            System.err.println(re.getMessage());
            return false;
        }

        return true;
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

    public int getTableSize() {
        return table.size();
    }

    public String getPlaintext() {
        return plaintext;
    }

    public int getChainLength() {
        return chainLength;
    }
}
