package keygenerators;

public abstract class KeyGenerator {

	// TODO: use 0-255 instead of constructor parameter?
	// TODO: 8 bytes? (already assumed in some places)
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
