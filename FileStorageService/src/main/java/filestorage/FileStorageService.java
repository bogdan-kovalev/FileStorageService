package filestorage;

import filestorage.impl.exception.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;

/**
 * @author Bogdan Kovalev
 */
public interface FileStorageService {

    /**
     * Saves the 'inputStream' into file in storage.
     *
     * @param key
     * @param inputStream
     * @throws StorageServiceIsNotStartedError
     * @throws NotEnoughFreeSpaceException
     * @throws StorageCorruptedException
     * @throws FileAlreadyExistsException
     */
    void saveFile(String key, InputStream inputStream)
            throws StorageServiceIsNotStartedError, NotEnoughFreeSpaceException, StorageCorruptedException, FileAlreadyExistsException;

    /**
     * Saves the 'inputStream' into file in storage. File will be deleted from storage after 'lifeTimeMillils' milliseconds.
     *
     * @param key
     * @param inputStream
     * @param lifeTimeMillis
     * @throws FileAlreadyExistsException
     * @throws StorageServiceIsNotStartedError
     * @throws NotEnoughFreeSpaceException
     * @throws StorageCorruptedException
     */
    void saveFile(String key, InputStream inputStream, long lifeTimeMillis)
            throws FileAlreadyExistsException, StorageServiceIsNotStartedError, NotEnoughFreeSpaceException, StorageCorruptedException;

    /**
     * Reads file with this 'key' from storage disk space
     *
     * @param key
     * @return InputStream
     * @throws StorageServiceIsNotStartedError
     * @throws FileNotFoundException
     */
    InputStream readFile(String key) throws StorageServiceIsNotStartedError, FileNotFoundException;

    /**
     * Deletes file with this 'key' from storage disk space.
     *
     * @param key
     * @throws StorageServiceIsNotStartedError
     * @throws MaybeFileInUseException
     */
    void deleteFile(String key) throws StorageServiceIsNotStartedError, MaybeFileInUseException;

    /**
     * @return free storage disk space in bytes
     * @throws StorageServiceIsNotStartedError
     */
    long getFreeStorageSpaceInBytes() throws StorageServiceIsNotStartedError;

    /**
     * @return free storage disk space in percents between 0.0 and 1.0
     * @throws StorageServiceIsNotStartedError
     */
    float getFreeStorageSpaceInPercents() throws StorageServiceIsNotStartedError;

    /**
     * This method releases free disk space by deleting old files.
     *
     * @param requiredFreeSpaceInPercents - The percentage of required free space from the total disk space.
     * @throws StorageServiceIsNotStartedError
     * @throws InvalidPercentsValueException
     */
    void purge(float requiredFreeSpaceInPercents) throws StorageServiceIsNotStartedError, InvalidPercentsValueException;

    /**
     * This method releases free disk space by deleting old files.
     *
     * @param requiredFreeSpaceInBytes - Required free space from the total disk space.
     * @throws StorageServiceIsNotStartedError
     * @throws InvalidPercentsValueException
     */
    void purge(long requiredFreeSpaceInBytes) throws StorageServiceIsNotStartedError;
}
