package file_storage.impl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bogdan Kovalev.
 */
public class LiveTimeWatcher implements Runnable {
    private final String filePath;
    private LiveTimeData liveTimeData;

    public LiveTimeWatcher(String storageRoot) {
        filePath = LiveTimeData.getFilePath(storageRoot);
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(filePath))) {
            liveTimeData = (LiveTimeData) stream.readObject();
        } catch (FileNotFoundException e) {
            new File(LiveTimeData.getDataFolderPath(storageRoot)).mkdir();
        } catch (Exception ignored) {
        }

        if (liveTimeData == null) liveTimeData = new LiveTimeData();
    }

    public void checkFiles() {
        final ConcurrentHashMap<String, Long> map = liveTimeData.getLiveTimeMap();
        for (String path : map.keySet()) {
            try {
                final File file = new File(path);
                final BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                final FileTime creationTime = attributes.creationTime();

                if (System.currentTimeMillis() - creationTime.toMillis() > map.get(path)) {
                    file.delete();
                    map.remove(path);
                }
            } catch (NoSuchFileException e) {
                map.remove(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveData();
    }

    public void addFile(String path, long liveTime) {
        liveTimeData.getLiveTimeMap().put(path, liveTime);
        saveData();
    }

    private void saveData() {
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(filePath))) {
            stream.writeObject(liveTimeData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            checkFiles();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                saveData();
            }
        }
    }

    public long getDataFileSize() throws IOException {
        final File file = new File(filePath);
        if (file.exists())
            return file.length();
        else {
            ByteArrayOutputStream byteObject = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(byteObject);
            outputStream.writeObject(liveTimeData);
            outputStream.flush();
            outputStream.close();
            byteObject.close();
            return byteObject.toByteArray().length;
        }
    }
}
