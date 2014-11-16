package filestorage.impl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Properties;

import static java.io.File.separator;

/**
 * @author Bogdan Kovalev.
 */
public class LifeTimeWatcher implements Runnable {

    public final static String DATA_FOLDER_NAME = "data";
    public final static String FILE_NAME = "storage.data";

    private final static String lifeTimeDataFilePath = separator.concat(DATA_FOLDER_NAME).concat(separator).concat(FILE_NAME);
    private final static String dataFolderPath = separator.concat(DATA_FOLDER_NAME);

    private final String filePath;
    private final StorageSpaceInspector storageSpaceInspector;

    private boolean run = true;

    private final Properties storageData = new Properties();

    public LifeTimeWatcher(String storageRoot, StorageSpaceInspector inspector) {
        storageSpaceInspector = inspector;

        filePath = storageRoot.concat(lifeTimeDataFilePath);

        try {
            storageData.load(new FileInputStream(filePath));
        } catch (FileNotFoundException ex) {
            new File(storageRoot.concat(dataFolderPath)).mkdir();
            //todo dir creation
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkFiles() {

        for (String path : storageData.stringPropertyNames()) {
            try {
                final File file = new File(path);
                final BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                final FileTime creationTime = attributes.creationTime();

                if (System.currentTimeMillis() - creationTime.toMillis() > Long.valueOf(storageData.getProperty(path))) {
                    file.delete();
                    storageData.remove(path);
                }
            } catch (NoSuchFileException e) {
                storageData.remove(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveData();
    }

    public void addFile(String path, long liveTime) {
        storageData.setProperty(path, String.valueOf(liveTime));
        saveData();
    }

    private void saveData() {
        if (!haveEnoughFreeSpaceToStore())
            //TODO not enough free space
            throw new IllegalStateException("not enough free space");

        try (final FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            storageData.store(fileOutputStream, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        final File file = new File(filePath);
        return file.length();
    }

    private synchronized boolean haveEnoughFreeSpaceToStore() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            storageData.store(outputStream, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File currentDataFile = new File(filePath);

        return outputStream.size() <
                // free storage space with taking account to space that will freed after current data file deleting
                storageSpaceInspector.getFreeSpace() + currentDataFile.length();
    }
}
