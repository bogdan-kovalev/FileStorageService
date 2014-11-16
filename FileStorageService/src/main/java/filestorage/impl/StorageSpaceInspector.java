package filestorage.impl;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * @author Bogdan Kovalev.
 */
public class StorageSpaceInspector implements Runnable {

    public static final int MINUTE = 1000 * 60;
    private final long maxDiskSpace;
    private final String rootFolder;

    private boolean run;

    private long freeSpace;

    public StorageSpaceInspector(long maxDiskSpace, String rootFolder) {
        this.maxDiskSpace = maxDiskSpace;
        this.rootFolder = rootFolder;

        analyzeFreeSpace();
    }

    @Override
    public void run() {
        while (run) {
            try {
                System.out.println("space inspector check");
                Thread.sleep(5 * MINUTE);
            } catch (InterruptedException e) {
                run = false;
            }
            analyzeFreeSpace();
        }
    }

    public void analyzeFreeSpace() {
        freeSpace = maxDiskSpace - FileUtils.sizeOfDirectory(new File(rootFolder));
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public void decrementFreeSpace(long bytes) {
        freeSpace -= bytes;
    }

    public void incrementFreeSpace(long bytes) {
        freeSpace += bytes;
    }
}
