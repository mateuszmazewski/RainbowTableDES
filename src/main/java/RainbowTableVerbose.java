import java.math.BigInteger;

public class RainbowTableVerbose extends RainbowTable {

    public RainbowTableVerbose(int passwordLength, int chainLength, String plaintext) {
        super(passwordLength, chainLength, plaintext);
    }

    @Override
    protected void generationThread(int count, int threadId, int threadCount) {
        long timeMillis = System.currentTimeMillis();

        super.generationThread(count, threadId, threadCount);

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Table-generating thread finished in " + seconds + "s");
    }

    @Override
    public void generate(int numChains, int threadCount) throws InterruptedException {
        long timeMillis = System.currentTimeMillis();

        super.generate(numChains, threadCount);

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Table generated in " + seconds + "s");
    }

    @Override
    public void generate(int numChains) {
        long timeMillis = System.currentTimeMillis();

        super.generate(numChains);

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
    public byte[] lookup(String cryptogramToCrack) {
        long timeMillis = System.currentTimeMillis();

        byte[] result = super.lookup(cryptogramToCrack);

        timeMillis = System.currentTimeMillis() - timeMillis;
        double seconds = timeMillis / 1000.0;
        System.out.println("Lookup took " + seconds + "s");
        return result;
    }
}
