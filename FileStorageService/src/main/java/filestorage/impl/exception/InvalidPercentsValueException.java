package filestorage.impl.exception;

import filestorage.impl.StorageException;

/**
 * @author Bogdan Kovalev.
 */
public class InvalidPercentsValueException extends StorageException {
    private final float invalidValue;

    public InvalidPercentsValueException(float invalidValue) {
        this.invalidValue = invalidValue;
    }

    @Override
    public String getMessage() {
        return "Value : " + invalidValue;
    }
}
