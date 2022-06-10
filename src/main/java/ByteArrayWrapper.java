import java.util.Arrays;

public class ByteArrayWrapper {

	private final byte[] array;

	public ByteArrayWrapper(byte[] array) {
		this.array = array;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ByteArrayWrapper) {
			return Arrays.equals(array, ((ByteArrayWrapper)o).get());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(array);
	}

	public byte[] get() {
		return array;
	}
}
