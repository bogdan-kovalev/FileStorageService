package file_storage;

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
    void saveFile(String key, InputStream inputStream) throws IOException;

    void saveFile(String key, InputStream inputStream, long liveTimeMillis) throws IOException;

    InputStream readFile(String key) throws FileNotFoundException;

    void deleteFile(String key) throws IOException;

    long getFreeStorageSpace();

    void purge(double percents);
}
