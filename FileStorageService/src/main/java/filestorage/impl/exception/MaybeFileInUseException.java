package filestorage.impl.exception;

import filestorage.impl.StorageException;

/**
 * @author Bogdan Kovalev.
 */
public class MaybeFileInUseException extends StorageException {
    private String key;

    public MaybeFileInUseException(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String getMessage() {
        return "Maybe file '" + key + "' in use";
    }
}
