package filestorage.impl;

import filestorage.impl.exception.NotEnoughFreeSpaceException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Properties;

/**
 * @author Bogdan Kovalev.
 */
public class LifeTimeWatcher implements Runnable {

    public final static String DATA_FOLDER_NAME = "system";
    public final static String FILE_NAME = "storage.data";

    private final String storageRoot;

    private final Path filePath;
    private final StorageSpaceInspector storageSpaceInspector;

    private boolean run = true;

    private final Properties storageData = new Properties();

    public LifeTimeWatcher(String storageRoot, StorageSpaceInspector inspector) throws IOException {
        this.storageRoot = storageRoot;
        storageSpaceInspector = inspector;

        filePath = Paths.get(storageRoot, DATA_FOLDER_NAME, FILE_NAME);

        if (Files.exists(filePath))
            storageData.load(new FileInputStream(filePath.toString()));
        else
            Files.createDirectories(Paths.get(storageRoot, DATA_FOLDER_NAME));
    }

    public void checkFiles() {

        for (String key : storageData.stringPropertyNames()) {
            Path path = Paths.get(PathConstructor.findDestinationPath(key, storageRoot), key);
            try {
                final FileTime creationTime = (FileTime) Files.getAttribute(path, "basic:creationTime");

                if (System.currentTimeMillis() - creationTime.toMillis() > Long.valueOf(storageData.getProperty(key))) {
                    Files.delete(path);
                    storageData.remove(key);
                }
            } catch (NoSuchFileException e) {
                storageData.remove(path);
            } catch (IOException e) {
                // TODO warning log
            }
        }

        saveData();
    }

    public void addFile(String key, long liveTime) throws NotEnoughFreeSpaceException {
        storageData.setProperty(key, String.valueOf(liveTime));
        saveData();
    }

    private void saveData() {
        if (!haveEnoughFreeSpaceToStore()) {
            System.out.println("Warning: Life time watcher store exception, haven't enough free space");
            return;
        }

        try (final FileOutputStream fileOutputStream = new FileOutputStream(String.valueOf(filePath))) {
            storageData.store(fileOutputStream, null);
        } catch (IOException e) {
            System.out.println("Warning: Life time watcher store exception");
        }
        storageSpaceInspector.dataFolderUpdated();
    }

    @Override
    public void run() {
        while (run) {
            checkFiles();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                run = false;
                saveData();
            }
        }
    }

    public long getDataFileSize() {
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            return calculateDataOutSize();
        }
    }

    private boolean haveEnoughFreeSpaceToStore() {
        long currentDataFileSize = new File(String.valueOf(filePath)).length();
        return calculateDataOutSize() <=
                // free storage space with taking account to space that will freed after current data file deleting
                storageSpaceInspector.getFreeSpace() + currentDataFileSize;
    }

    private long calculateDataOutSize() {
        long outSize = 0;
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            storageData.store(outputStream, null);
            outSize = outputStream.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outSize;
    }
}
