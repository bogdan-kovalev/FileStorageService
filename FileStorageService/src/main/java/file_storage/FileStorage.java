package file_storage;

import java.io.InputStream;

/**
 * @author Bogdan Kovalev
 */
public interface FileStorage {
    void saveFile(String key, InputStream inputStream);
    void saveFile(String key, InputStream inputStream, long liveTimeMillis);
    InputStream readFile(String key);

    void deleteFile(String key);
    double getFreeStorageSpace();
    void purge(double percents);
}
