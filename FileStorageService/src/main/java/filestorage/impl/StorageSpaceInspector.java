package filestorage.impl;

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
        freeSpace = maxDiskSpace - calcSizeAndClearEmpties(new File(rootFolder));
    }

    public static long calcSizeAndClearEmpties(File directory) {
        long size = 0L;
        if (!directory.exists()) {
            return size;
        } else if (!directory.isDirectory()) {
            return size;
        } else {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    size += calcSizeAndClearEmpties(file);
                } else {
                    size += file.length();
                }
            }
            if (directory.listFiles().length == 0) {
                directory.delete();
            }
            return size;
        }
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
