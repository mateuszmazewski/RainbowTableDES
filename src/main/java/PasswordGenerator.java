public abstract class PasswordGenerator {

	protected final char[] charset;

	public PasswordGenerator(char[] charset) {
		this.charset = charset;
	}

	public abstract String next(int arg);

}
