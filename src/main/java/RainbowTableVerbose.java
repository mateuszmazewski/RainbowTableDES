import java.math.BigInteger;

public class RainbowTableVerbose extends RainbowTable {

    public RainbowTableVerbose(String charset, int passwordLength, int chainLength, int numChains, HashAlgorithm hashAlgorithm) {
        super(charset, passwordLength, chainLength, numChains, hashAlgorithm);
    }

    @Override
    protected void generationThread(int count) {
        long timeMillis = System.currentTimeMillis();

        super.generationThread(count);

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Table-generating thread finished in " + seconds + "s");
    }

    @Override
    public void generate(int threadCount) throws InterruptedException {
        long timeMillis = System.currentTimeMillis();

        super.generate(threadCount);

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Table generated in " + seconds + "s");
    }

    @Override
    public void generate() {
        long timeMillis = System.currentTimeMillis();

        super.generate();

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Table generated in " + seconds + "s");
    }

    @Override
    protected BigInteger getPrimeModulus() {
        BigInteger primeModulus = super.getPrimeModulus();
        System.out.println("prime modulus: " + primeModulus);
        return primeModulus;
    }

    @Override
    public String lookup(String cipherTextToCrack) {
        long timeMillis = System.currentTimeMillis();

        String result = super.lookup(cipherTextToCrack);

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Lookup took " + seconds + "s");
        return result;
    }
}
