package keygenerators;

public abstract class KeyGenerator {

	protected final byte[] byteset;

	public KeyGenerator() {
		this.byteset = new byte[256];
		for(int i = 0; i < 256; i++) {
			byteset[i] = (byte) i;
		}
	}

	public KeyGenerator(byte[] byteset) {
		this.byteset = byteset;
	}

	public abstract byte[] next(Object arg);

}
