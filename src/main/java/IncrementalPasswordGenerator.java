public class IncrementalPasswordGenerator extends PasswordGenerator {

	private int counter;

	public IncrementalPasswordGenerator(char[] charset) {
		this(charset, 0);
	}

	public IncrementalPasswordGenerator(char[] charset, int startValue) {
		super(charset);
		this.counter = startValue;
	}

	@Override
	public String next(int increment) {
		int counterCurrent = counter;
		StringBuilder stringBuilder = new StringBuilder();
		while (counterCurrent > 0) {
			stringBuilder.append(charset[(counterCurrent - 1) % charset.length]);
			counterCurrent /= charset.length;
		}

		counter += increment;
		return stringBuilder.reverse().toString();
	}
}
