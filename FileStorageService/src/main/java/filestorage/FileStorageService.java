package filestorage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Bogdan Kovalev
 */
public interface FileStorageService {

    void saveFile(String key, InputStream inputStream) throws IOException, StorageException;

    void saveFile(String key, InputStream inputStream, long liveTimeMillis) throws IOException, StorageException;

    InputStream readFile(String key) throws FileNotFoundException, StorageException;

    void deleteFile(String key) throws IOException, StorageException;

    long getFreeStorageSpace() throws StorageException;

    void purge(float percents) throws StorageException;
}
