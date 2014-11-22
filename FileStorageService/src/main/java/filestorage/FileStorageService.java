package filestorage;

import filestorage.impl.exception.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;

/**
 * @author Bogdan Kovalev
 */
public interface FileStorageService {

    void saveFile(String key, InputStream inputStream)
            throws StorageServiceIsNotStartedError, NotEnoughFreeSpaceException, StorageCorruptedException,
            FileLockedException, FileAlreadyExistsException;

    void saveFile(String key, InputStream inputStream, long liveTimeMillis)
            throws FileAlreadyExistsException, StorageServiceIsNotStartedError, NotEnoughFreeSpaceException,
            FileLockedException, StorageCorruptedException;

    InputStream readFile(String key) throws StorageServiceIsNotStartedError, FileNotFoundException;

    void deleteFile(String key) throws StorageServiceIsNotStartedError;

    long getFreeStorageSpace() throws StorageServiceIsNotStartedError;

    void purge(float percents) throws StorageServiceIsNotStartedError, InvalidPercentsValueException;
}
