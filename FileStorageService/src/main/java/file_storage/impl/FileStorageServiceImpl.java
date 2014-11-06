package file_storage.impl;


import file_storage.FileStorageService;

import java.io.File;
import java.io.InputStream;

/**
 * @author Bogdan Kovalev
 */
public class FileStorageServiceImpl implements FileStorageService {

    private final long maxDiskSpace;
    private final File rootFolder;

    /**
     * @param maxDiskSpace max disk space can be used (in bytes)
     * @param rootFolder   root folder of file system
     */
    public FileStorageServiceImpl(long maxDiskSpace, File rootFolder) {
        this.maxDiskSpace = maxDiskSpace;
        this.rootFolder = rootFolder;
    }

    @Override
    public void saveFile(String key, InputStream inputStream) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void saveFile(String key, InputStream inputStream, long liveTimeMillis) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public InputStream readFile(String key) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void deleteFile(String key) {
        throw new IllegalStateException("Not implemented yet");
    }

    /**
     * @return free storage space in bytes
     */
    @Override
    public double getFreeStorageSpace() {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void purge(double percents) {
        throw new IllegalStateException("Not implemented yet");
    }
}
