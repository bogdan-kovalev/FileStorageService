package filestorage.impl.exception;

/**
 * Checked exception thrown when percentage is not in 0.0 ... 1.0 range.
 *
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
