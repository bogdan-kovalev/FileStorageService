package filestorage.impl.exception;

import filestorage.impl.StorageException;

/**
 * Checked exception thrown when storage service can't perform a file operation because of directories in file storage
 * was directly changed.
 *
 * @author Bogdan Kovalev
 */
public class StorageCorruptedException extends StorageException {
}
