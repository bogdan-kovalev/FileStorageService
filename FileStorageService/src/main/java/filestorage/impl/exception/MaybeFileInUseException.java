package filestorage.impl.exception;

/**
 * @author Bogdan Kovalev.
 */
public class MaybeFileInUseException extends Exception {
    private String key;

    public MaybeFileInUseException(String key) {
        super("Maybe file '" + key + "' in use");
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
