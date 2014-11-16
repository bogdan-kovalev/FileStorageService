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
public class LiveTimeWatcher implements Runnable {

    public final static String DATA_FOLDER_NAME = "data";
    public final static String FILE_NAME = "storage.data";

    private final static String liveTimeDataFilePath = separator.concat(DATA_FOLDER_NAME).concat(separator).concat(FILE_NAME);
    private final static String dataFolderPath = separator.concat(DATA_FOLDER_NAME);

    private final String filePath;
    private final StorageSpaceInspector storageSpaceInspector;

    private boolean run = true;

    private final Properties storageData = new Properties();

    public LiveTimeWatcher(String storageRoot, StorageSpaceInspector inspector) {
        storageSpaceInspector = inspector;

        filePath = storageRoot.concat(liveTimeDataFilePath);

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
        if (storageSpaceInspector.getFreeSpace() < getDataObjectSize())
            return;

        try (final FileOutputStream outputStream = new FileOutputStream(filePath)) {
            storageData.store(outputStream, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (run) {
            try {
                checkFiles();
                Thread.sleep(500);
            } catch (InterruptedException e) {
                run = false;
                saveData();
            }
        }
    }

    public long getDataFileSize() {
        final File file = new File(filePath);
        if (file.exists())
            return file.length();
        else
            return getDataObjectSize();
    }

    private long getDataObjectSize() {
        ByteArrayOutputStream byteObject = new ByteArrayOutputStream();
        try (final ObjectOutputStream outputStream = new ObjectOutputStream(byteObject)) {
            outputStream.writeObject(storageData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteObject.toByteArray().length;
    }
}
