import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public interface HashAlgorithm {

    String hash(String text) throws BadPaddingException, IllegalBlockSizeException;

}
