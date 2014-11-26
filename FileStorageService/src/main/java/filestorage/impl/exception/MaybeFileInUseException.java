package filestorage.impl.exception;

import filestorage.impl.StorageException;

/**
 * Checked exception thrown when deleting of a file can not be provided, because another thread currently reading this file.
 *
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
