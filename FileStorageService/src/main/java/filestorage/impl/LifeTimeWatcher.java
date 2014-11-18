package filestorage.impl;

import filestorage.impl.exception.NotEnoughFreeSpaceException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Properties;

import static filestorage.impl.FileStorageServiceImpl.DATA_FOLDER_NAME;
import static filestorage.impl.FileStorageServiceImpl.SYSTEM_FOLDER_NAME;

/**
 * @author Bogdan Kovalev.
 */
public class LifeTimeWatcher implements Runnable {

    public static final int SLEEP_TIME = 500;
    private final String STORAGE_ROOT;

    private final Path systemFilePath;
    private final StorageSpaceInspector storageSpaceInspector;

    private boolean run = true;

    private final Properties systemData = new Properties();

    public LifeTimeWatcher(String STORAGE_ROOT, StorageSpaceInspector inspector) throws IOException {
        this.STORAGE_ROOT = STORAGE_ROOT;
        storageSpaceInspector = inspector;

        systemFilePath = Paths.get(STORAGE_ROOT, SYSTEM_FOLDER_NAME, FileStorageServiceImpl.SYSTEM_FILE_NAME);

        if (Files.exists(systemFilePath))
            systemData.load(new FileInputStream(String.valueOf(systemFilePath)));
        else
            Files.createDirectories(Paths.get(STORAGE_ROOT, SYSTEM_FOLDER_NAME));

        deleteExpiredFiles();
    }

    public void deleteExpiredFiles() {

        for (String key : systemData.stringPropertyNames()) {
            Path path = Paths.get(STORAGE_ROOT, PathConstructor.findDestinationPath(key, DATA_FOLDER_NAME), key);
            try {
                final FileTime creationTime = (FileTime) Files.getAttribute(path, "basic:creationTime");

                if (System.currentTimeMillis() - creationTime.toMillis() > Long.valueOf(systemData.getProperty(key))) {
                    Files.delete(path);
                    systemData.remove(key);
                }
            } catch (NoSuchFileException e) {
                systemData.remove(key);
            } catch (IOException e) {
                // TODO warning log
            }
        }

        storeSystemData();
    }

    public void addFile(String key, long liveTime) throws NotEnoughFreeSpaceException {
        systemData.setProperty(key, String.valueOf(liveTime));
        storeSystemData();
    }

    private void storeSystemData() {
        if (!haveEnoughFreeSpaceToStore()) {
            System.out.println("Warning: Life time watcher store exception, haven't enough free space");
            return;
        }

        long sizeBefore = new File(String.valueOf(systemFilePath)).length();

        try (final FileOutputStream fileOutputStream = new FileOutputStream(String.valueOf(systemFilePath))) {
            systemData.store(fileOutputStream, null);
        } catch (IOException e) {
            System.out.println("Warning: Life time watcher store exception");
        }

        long currentSize = new File(String.valueOf(systemFilePath)).length();

        storageSpaceInspector.decrementUsedSpace(sizeBefore);
        storageSpaceInspector.incrementUsedSpace(currentSize);
    }

    @Override
    public void run() {
        while (run) {
            try {
                Thread.sleep(SLEEP_TIME);
                deleteExpiredFiles();
            } catch (InterruptedException e) {
                run = false;
                storeSystemData();
            }
        }
    }

    public long getSystemDataFileSize() {
        try {
            return Files.size(systemFilePath);
        } catch (IOException e) {
            return calculateDataOutSize();
        }
    }

    private boolean haveEnoughFreeSpaceToStore() {
        long currentDataFileSize = new File(String.valueOf(systemFilePath)).length();
        return calculateDataOutSize() <=
                // free storage space with taking account to space that will freed after current data file deleting
                storageSpaceInspector.getFreeSpace() + currentDataFileSize;
    }

    private long calculateDataOutSize() {
        long outSize = 0;
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            systemData.store(outputStream, null);
            outSize = outputStream.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outSize;
    }
}
