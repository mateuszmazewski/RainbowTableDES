package keygenerators;

import java.util.Random;

public class RandomKeyGenerator extends KeyGenerator {

	private final Random random;

	public RandomKeyGenerator(byte[] byteset) {
		super(byteset);
		this.random = new Random();
	}

	@Override
	public byte[] next(Object ignored) {
		byte[] key = new byte[8];
		for (int i = 0; i < 8; i++) {
			key[i] = byteset[(int) (random.nextDouble() * byteset.length)];
		}

		return key;
	}
}
