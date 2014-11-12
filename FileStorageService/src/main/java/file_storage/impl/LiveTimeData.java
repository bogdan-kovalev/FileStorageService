package file_storage.impl;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;

/**
 * @author Bogdan Kovalev.
 */
public class LiveTimeData implements Serializable {
    public final static String DATA_FOLDER_NAME = "data";
    public final static String FILE_NAME = "liveTimes.dat";

    private final static String liveTimeDataFilePath = separator.concat(DATA_FOLDER_NAME).concat(separator).concat(FILE_NAME);
    private final static String dataFolderPath = separator.concat(DATA_FOLDER_NAME);

    private ConcurrentHashMap<String, Long> liveTimeMap;

    public LiveTimeData() {
        this.liveTimeMap = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMap<String, Long> getLiveTimeMap() {
        return liveTimeMap;
    }

    public static String getFilePath(String storageRoot) {
        return storageRoot.concat(liveTimeDataFilePath);
    }

    public static String getDataFolderPath(String storageRoot) {
        return storageRoot.concat(dataFolderPath);
    }
}
