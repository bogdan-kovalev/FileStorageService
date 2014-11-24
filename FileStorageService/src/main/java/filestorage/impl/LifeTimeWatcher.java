package filestorage.impl;

import filestorage.impl.exception.NotEnoughFreeSpaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Properties;

import static filestorage.impl.DefaultFileStorageService.DATA_FOLDER_NAME;
import static filestorage.impl.DefaultFileStorageService.SYSTEM_FOLDER_NAME;

/**
 * This class provides possibility to delete expired files.
 *
 * @author Bogdan Kovalev.
 */
public class LifeTimeWatcher implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LifeTimeWatcher.class);

    public static final int SLEEP_TIME = 500;

    private final String STORAGE_ROOT;

    private final Path systemFilePath;
    private final StorageSpaceInspector storageSpaceInspector;

    private boolean run = true;

    private final Properties systemData = new Properties();

    public LifeTimeWatcher(String STORAGE_ROOT, StorageSpaceInspector inspector) throws IOException {
        this.STORAGE_ROOT = STORAGE_ROOT;
        storageSpaceInspector = inspector;

        systemFilePath = Paths.get(STORAGE_ROOT, SYSTEM_FOLDER_NAME, DefaultFileStorageService.SYSTEM_FILE_NAME);

        if (Files.exists(systemFilePath))
            systemData.load(new FileInputStream(String.valueOf(systemFilePath)));
        else
            Files.createDirectories(Paths.get(STORAGE_ROOT, SYSTEM_FOLDER_NAME));

        deleteExpiredFiles();
    }

    /**
     * This method check files listed in the system.data file and delete all expired files from the storage.
     */
    public void deleteExpiredFiles() {

        for (String key : systemData.stringPropertyNames()) {
            Path path = Paths.get(STORAGE_ROOT, PathConstructor.calculateDestinationPath(key, DATA_FOLDER_NAME), key);
            try {
                final FileTime creationTime = (FileTime) Files.getAttribute(path, "basic:creationTime");

                if (System.currentTimeMillis() - creationTime.toMillis() > Long.valueOf(systemData.getProperty(key))) {
                    Files.delete(path);
                    systemData.remove(key);
                    if (LOG.isInfoEnabled())
                        LOG.info("Expired file '{}' successfully deleted.", key);
                }
            } catch (NoSuchFileException e) {
                systemData.remove(key);
            } catch (IOException e) {
                if (LOG.isWarnEnabled())
                    LOG.warn("Expired file '{}' was not deleted because of IOException.", key);
            }
        }

        storeSystemData();
    }

    /**
     * This method adds file with this key and life time to the system.data file.
     *
     * @param key
     * @param liveTime
     * @throws NotEnoughFreeSpaceException
     */
    public void addFile(String key, long liveTime) throws NotEnoughFreeSpaceException {
        systemData.setProperty(key, String.valueOf(liveTime));
        storeSystemData();
    }

    private void storeSystemData() {
        if (!haveEnoughFreeSpaceToStore()) {
            if (LOG.isWarnEnabled())
                LOG.warn("Haven't enough free space to store system data");
            return;
        }

        long sizeBefore = new File(String.valueOf(systemFilePath)).length();

        try (final FileOutputStream fileOutputStream = new FileOutputStream(String.valueOf(systemFilePath))) {
            systemData.store(fileOutputStream, null);
        } catch (IOException e) {
            if (LOG.isWarnEnabled())
                LOG.warn("System data storing failed");
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
                synchronized (this) {
                    run = false;
                    storeSystemData();
                    notify();
                }
            }
        }
    }

    private boolean haveEnoughFreeSpaceToStore() {
        long currentDataFileSize = new File(String.valueOf(systemFilePath)).length();
        return calculateDataOutSize() <=
                // free storage space with taking account to space that will freed after current data file deleting
                storageSpaceInspector.getFreeSpace() + currentDataFileSize;
    }

    /**
     * Returns size of systemData object in bytes.
     *
     * @return
     */
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
