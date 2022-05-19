import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RainbowTable {
    private final char[] charset;
    private final int passwordLength;
    private final int chainLength;
    private final int numChains;
    private final BigInteger modulo;
    private final ChainCollection chainCollection;
    private final HashAlgorithm hashAlgorithm;
    private final Random random;
    private final Set<String> usedStartHashes;

    public RainbowTable(String charset, int passwordLength, int chainLength, int numChains, HashAlgorithm hashAlgorithm) {
        this.charset = charset.toCharArray();
        this.passwordLength = passwordLength;
        this.chainLength = chainLength;
        this.numChains = numChains;
        this.modulo = getPrimeModulus();
        this.chainCollection = new ChainCollection();
        this.hashAlgorithm = hashAlgorithm;
        this.random = new Random();
        this.usedStartHashes = ConcurrentHashMap.newKeySet();
    }

    protected void generationThread(int count) {
        String startPass, endPass;
        int generatedChains = 0;

        while (generatedChains < count) {
            startPass = generatePassword(passwordLength);
            endPass = generateChain(startPass);
            chainCollection.add(startPass, endPass);
            generatedChains++;
        }
    }

    public void generate(int threadCount) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];

        for(int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                // TODO: include remainder
                // TODO: active assignment instead of fixed count per process?
                generationThread(numChains / threadCount);
            });
            threads[i].start();
        }

        for(Thread t : threads) {
            t.join();
        }
    }

    public void generate() {
        generationThread(numChains);
    }

    // TODO: find a better way
    private String generatePassword(int passwordLength) {
        //return generateRandomPassword(passwordLength);
        String password, hash = "";
        do {
            password = generateRandomPassword(passwordLength);
            try {
                hash = hashAlgorithm.hash(password);
            } catch (BadPaddingException | IllegalBlockSizeException ignored) {}
        } while (usedStartHashes.contains(hash));
        usedStartHashes.add(hash);
        return password;
    }

    // TODO: randomness yields inconsistent results
    private String generateRandomPassword(int passwordLength) {
        StringBuilder sb = new StringBuilder(passwordLength);

        for (int i = 0; i < passwordLength; i++) {
            sb.append(charset[(int) (random.nextDouble() * charset.length)]);
        }

        return sb.toString();
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
        BigInteger index;
        StringBuilder sb = new StringBuilder();

        // Convert hex string into decimal value
        BigInteger temp = new BigInteger(cipherText, 16);
        // Reduction output depends on the chain position
        temp = temp.add(BigInteger.valueOf(position));
        temp = temp.mod(modulo);

        for (int i = 0; i < passwordLength; i++) {
            index = temp.mod(BigInteger.valueOf(charset.length));
            sb.append(charset[index.intValue()]);
            temp = temp.divide(BigInteger.valueOf(charset.length));
        }

        return sb.toString();
    }

    protected BigInteger getPrimeModulus() {
        return BigInteger.valueOf(charset.length).pow(passwordLength);
    }

    public double saveTableToFile(String pathname) {
        if (chainCollection.size() == 0) {
            throw new IllegalStateException("Table not generated");
        }

        File out = new File(pathname);
        FileWriter fw;
        long timeMillis = System.currentTimeMillis();

        try {
            fw = new FileWriter(out);
            for (ChainCollection.PasswordPair pp : chainCollection.getPasswordPairs()) {
                fw.write(pp.getStartPassword() + " " + pp.getEndPassword() + "\n");
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

            Set<String> startPasswords = chainCollection.getStartPasswords(endPass);
            for (String startPass : startPasswords) {
                lookup = lookupChain(startPass, cipherTextToCrack);
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
