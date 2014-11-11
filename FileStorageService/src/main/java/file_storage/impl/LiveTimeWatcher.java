package file_storage.impl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;

/**
 * @author Bogdan Kovalev.
 */
public class LiveTimeWatcher implements Runnable {
    private final static String dataFolder = "data";
    private final static String fileName = "liveTimes.dat";
    private final String dataFilePath;
    private FilesLiveTimeMap map;

    public LiveTimeWatcher(String storageRoot) {
        new File(storageRoot + separator + dataFolder).mkdir();
        dataFilePath = storageRoot + separator + dataFolder + separator + fileName;
        if ((new File(dataFilePath).exists())) {
            try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(dataFilePath))) {
                map = (FilesLiveTimeMap) stream.readObject();
            } catch (Exception e) {
                map = new FilesLiveTimeMap();
            }
        } else {
            map = new FilesLiveTimeMap();
        }
    }

    public void checkFiles() {
        for (String path : map.getMap().keySet()) {
            try {
                final File file = new File(path);
                final BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                final FileTime fileTime = attributes.creationTime();
                if (System.currentTimeMillis() - fileTime.toMillis() > map.getMap().get(path)) {
                    file.delete();
                    map.getMap().remove(path);
                }
            } catch (NoSuchFileException e) {
                map.getMap().remove(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addFile(String path, long liveTime) {
        map.getMap().put(path, liveTime);
        saveData();
    }

    private void saveData() {
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(dataFilePath))) {
            stream.writeObject(map);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
                e.printStackTrace();
            }
        }
    }
}

class FilesLiveTimeMap implements Serializable {
    private ConcurrentHashMap<String, Long> filesLiveTimeMap;

    public FilesLiveTimeMap() {
        filesLiveTimeMap = new ConcurrentHashMap<>();
    }

    public Map<String, Long> getMap() {
        return filesLiveTimeMap;
    }
}
