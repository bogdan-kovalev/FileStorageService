package file_storage;

import java.io.InputStream;

/**
 * @author Bogdan Kovalev
 */
public interface FileStorage {
    void saveFile(String key, InputStream input);
    InputStream readFile(String key);
    long getFreeStorageSpace();
}
