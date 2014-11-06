package file_storage.impl;


import file_storage.FileStorage;

import java.io.File;
import java.io.InputStream;

/**
 * @author Bogdan Kovalev
 */
public class FileStorageImpl implements FileStorage {

    private final long maxDiskSpace;
    private final File rootFolder;

    /**
     *
     * @param maxDiskSpace max disk space can be used (in bytes)
     * @param rootFolder root folder of file system
     */
    public FileStorageImpl(long maxDiskSpace, File rootFolder) {
        this.maxDiskSpace = maxDiskSpace;
        this.rootFolder = rootFolder;
    }

    /**
     *
     * @param key unique id
     * @param input input stream
     */
    @Override
    public void saveFile(String key, InputStream input) {

    }

    @Override
    public InputStream readFile(String key) {
        return null;
    }

    /**
     *
     * @return free storage space in bytes
     */
    @Override
    public long getFreeStorageSpace() {
        return 0;
    }
}
