package keygenerators;

import java.math.BigInteger;

public class IncrementalKeyGenerator extends KeyGenerator {

	private BigInteger counter;
	private final BigInteger bytesetLength; // stored for performance reasons


	public IncrementalKeyGenerator(BigInteger startValue) {
		super(); // NOTE: generates byteset of size 256, used below
		this.counter = startValue;
		this.bytesetLength = BigInteger.valueOf(256);
	}

	public IncrementalKeyGenerator(byte[] byteset, BigInteger startValue) {
		super(byteset);
		this.counter = startValue;
		this.bytesetLength = BigInteger.valueOf(byteset.length);
	}
	public IncrementalKeyGenerator() {
		this(BigInteger.ZERO);
	}

	public IncrementalKeyGenerator(byte[] byteset) {
		this(byteset, BigInteger.ZERO);
	}

	public IncrementalKeyGenerator(byte[] byteset, long startValue) {
		this(byteset, BigInteger.valueOf(startValue));
	}

	@Override
	public byte[] next(Object incrementObject) {
		Long increment = (Long) incrementObject;
		byte[] key = new byte[8];
		BigInteger counterCurrent = counter;
		for (int i = 0; i < 8; i++) {
			key[i] = byteset[counterCurrent.mod(bytesetLength).intValue()];
			counterCurrent = counterCurrent.divide(bytesetLength);
		}

		counter = counter.add(BigInteger.valueOf(increment));
		return key;
	}
}
