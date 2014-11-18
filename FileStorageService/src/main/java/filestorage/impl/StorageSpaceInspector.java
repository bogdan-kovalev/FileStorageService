package filestorage.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Bogdan Kovalev.
 */
public class StorageSpaceInspector {

    private final long maxDiskSpace;
    private final String STORAGE_ROOT;

    private long usedSpace;

    private TreeSet<Path> purgeSet = new TreeSet<>(new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            final Path file1 = (Path) o1;
            final Path file2 = (Path) o2;
            try {
                final FileTime creationTime1 = (FileTime) Files.getAttribute(file1, "basic:creationTime");
                final FileTime creationTime2 = (FileTime) Files.getAttribute(file2, "basic:creationTime");
                final int cmp = creationTime1.compareTo(creationTime2);
                return cmp == 0 ? 1 : cmp;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }
    });

    private Consumer<Path> incrementUsedSpace = new Consumer<Path>() {
        @Override
        public void accept(Path path) {
            final File file = new File(String.valueOf(path));
            if (file.isFile())
                usedSpace += file.length();
        }
    };

    private Consumer<Path> prepareForPurge = new Consumer<Path>() {
        @Override
        public void accept(Path path) {
            if (path.endsWith(FileStorageServiceImpl.SYSTEM_FILE_NAME)) return;
            final File file = new File(String.valueOf(path));
            if (file.isFile()) {
                purgeSet.add(path);
            }
        }
    };

    public StorageSpaceInspector(long maxDiskSpace, String STORAGE_ROOT) {
        this.maxDiskSpace = maxDiskSpace;
        this.STORAGE_ROOT = STORAGE_ROOT;

        evaluateUsedSpace();
    }

    private void evaluateUsedSpace() {
        usedSpace = 0;
        performInStorage(incrementUsedSpace);
    }

    public void clearEmptyDirectories(File start) {
        if (!start.isDirectory()) return;

        for (File file : start.listFiles()) {
            if (file.isFile()) return;

            if (file.isDirectory())
                clearEmptyDirectories(file);
        }
        start.delete();
    }

    public void purge(long neededFreeSpace) {
        if (getFreeSpace() >= neededFreeSpace) return;

        purgeSet.clear();
        performInStorage(prepareForPurge);

        while (!purgeSet.isEmpty() && getFreeSpace() < neededFreeSpace) {
            final Path oldestFile = purgeSet.pollFirst();
            try {
                final long size = Files.size(oldestFile);
                Files.delete(oldestFile);
                decrementUsedSpace(size);
            } catch (IOException ignored) {
            }
        }
    }

    private void performInStorage(Consumer<Path> consumer) {
        try (final Stream<Path> walk = Files.walk(Paths.get(STORAGE_ROOT))) {
            walk.forEach(consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getFreeSpace() {
        return maxDiskSpace - usedSpace;
    }

    public void incrementUsedSpace(long bytes) {
        usedSpace += bytes;
    }

    public void decrementUsedSpace(long bytes) {
        usedSpace -= bytes;
    }

    public long getSystemFolderSize() {
        long size = 0;
        String systemFolderPath = String.valueOf(Paths.get(STORAGE_ROOT, FileStorageServiceImpl.SYSTEM_FOLDER_NAME));

        final File[] files = new File(systemFolderPath).listFiles();
        if (files != null)
            for (File file : files) {
                size += file.length();
            }
        return size;
    }
}
