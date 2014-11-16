package filestorage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Bogdan Kovalev
 */
public interface FileStorageService {
    /**
     * @param key         unique id
     * @param inputStream input stream
     */
    void saveFile(String key, InputStream inputStream) throws IOException, StorageException;

    /**
     * @param key            unique id
     * @param inputStream    input stream
     * @param liveTimeMillis live time of the stored file
     * @throws IOException
     */
    void saveFile(String key, InputStream inputStream, long liveTimeMillis) throws IOException, StorageException;

    InputStream readFile(String key) throws FileNotFoundException, StorageException;

    void deleteFile(String key) throws IOException, StorageException;

    long getFreeStorageSpace() throws StorageException;

    void purge(double percents) throws StorageException;
}
