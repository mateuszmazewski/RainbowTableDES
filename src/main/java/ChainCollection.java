import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChainCollection {

	// collection requirements:
	// - add startPassword and endPassword simultaneously
	// - check whether endPasswords contains given element
	// - get startPassword by endPassword
	// - thread safety
	// - endPasswords duplicates allowed

	private final List<PasswordPair> passwordPairs;
	private final Object lock = new Object();

	public static class PasswordPair {
		private String startPassword;
		private String endPassword;

		public PasswordPair(String startPassword, String endPassword) {
			this.startPassword = startPassword;
			this.endPassword = endPassword;
		}

		public String getStartPassword() {
			return startPassword;
		}

		public void setStartPassword(String startPassword) {
			this.startPassword = startPassword;
		}

		public String getEndPassword() {
			return endPassword;
		}

		public void setEndPassword(String endPassword) {
			this.endPassword = endPassword;
		}
	}

	public ChainCollection() {
		this.passwordPairs = new ArrayList<>();
	}

	public void add(String startPassword, String endPassword) {
		synchronized (lock) {
			passwordPairs.add(new PasswordPair(startPassword, endPassword));
		}
	}

	// TODO: optimize
	public Set<String> getStartPasswords(String endPassword) {
		synchronized (lock) {
			return passwordPairs.stream()
				.filter(pp -> pp.endPassword.equals(endPassword))
				.map(pp -> pp.startPassword)
				.collect(Collectors.toSet());
		}
	}

	public List<PasswordPair> getPasswordPairs() {
		synchronized (lock) {
			return passwordPairs;
		}
	}

	public int size() {
		synchronized (lock) {
			return passwordPairs.size();
		}
	}

}
