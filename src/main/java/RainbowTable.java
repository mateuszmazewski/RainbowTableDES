import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RainbowTable {
    private final char[] charset;
    private final int passwordLength;
    private final int chainLength;
    private final int numChains;
    private final BigInteger modulo;
    private final ChainCollection chainCollection;
    private final DES des;
    private final Set<String> usedStartCipherTexts;

    public RainbowTable(String charset, int passwordLength, int chainLength, int numChains, DES des) {
        this.charset = charset.toCharArray();
        this.passwordLength = passwordLength;
        this.chainLength = chainLength;
        this.numChains = numChains;
        this.modulo = getPrimeModulus();
        this.chainCollection = new ChainCollection();
        this.des = des;
        this.usedStartCipherTexts = ConcurrentHashMap.newKeySet();
    }

    protected void generationThread(int count, int threadId, int threadCount) {
        PasswordGenerator passwordGenerator = new IncrementalPasswordGenerator(charset, threadId + 1);
        String startPass, endPass;
        int generatedChains = 0;

        while (generatedChains < count) {
            startPass = generatePassword(threadCount, passwordGenerator);
            endPass = generateChain(startPass);
            chainCollection.add(startPass, endPass);
            generatedChains++;
        }
    }

    public void generate(int threadCount) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> {
                // TODO: include remainder
                // TODO: active assignment instead of fixed count per process?
                generationThread(numChains / threadCount, finalI, threadCount);
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }
    }

    public void generate() {
        generationThread(numChains, 0, 1);
    }

    private String generatePassword(int arg, PasswordGenerator passwordGenerator) {
        String password, cipherText = "";
        do {
            password = passwordGenerator.next(arg);
            try {
                cipherText = des.encrypt(password);
            } catch (BadPaddingException | IllegalBlockSizeException ignored) {
            }
        } while (usedStartCipherTexts.contains(cipherText));
        usedStartCipherTexts.add(cipherText);
        return password;
    }

    private String generateChain(String startPass) {
        String cipherText, endPass = startPass;

        try {
            for (int i = 0; i < chainLength; i++) {
                cipherText = des.encrypt(endPass);
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
                    cipherText = des.encrypt(endPass);
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

}
