package keygenerators;

public class IncrementalKeyGenerator extends KeyGenerator {

	private long counter;

	public IncrementalKeyGenerator() {
		this(0);
	}

	public IncrementalKeyGenerator(long startValue) {
		super();
		this.counter = startValue;
	}

	public IncrementalKeyGenerator(byte[] byteset) {
		this(byteset, 0);
	}

	public IncrementalKeyGenerator(byte[] byteset, long startValue) {
		super(byteset);
		this.counter = startValue;
	}

	@Override
	public byte[] next(Object incrementObject) {
		Long increment = (Long) incrementObject;
		byte[] key = new byte[8];
		long counterCurrent = counter;
		for (int i = 0; i < 8; i++) {
			key[i] = byteset[(int) (counterCurrent % byteset.length)];
			counterCurrent /= byteset.length;
		}

		counter += increment;
		return key;
	}
}
