import java.util.Random;

public class RandomPasswordGenerator extends PasswordGenerator {

	private final Random random;

	public RandomPasswordGenerator(char[] charset) {
		super(charset);
		this.random = new Random();
	}

	@Override
	public String next(int passwordLength) {
		StringBuilder sb = new StringBuilder(passwordLength);

		for (int i = 0; i < passwordLength; i++) {
			sb.append(charset[(int) (random.nextDouble() * charset.length)]);
		}

		return sb.toString();
	}
}
