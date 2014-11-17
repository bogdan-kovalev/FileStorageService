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
public class StorageSpaceInspector implements Runnable {

    public static final int MINUTE = 1000 * 60;
    private final long maxDiskSpace;
    private final String rootFolder;

    private boolean run;

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

    private Consumer<Path> filesSizeConsumer = new Consumer<Path>() {
        @Override
        public void accept(Path path) {
            final File file = new File(String.valueOf(path));
            if (file.isFile())
                usedSpace += file.length();
        }
    };

    private Consumer<Path> deleteIfEmptyConsumer = new Consumer<Path>() {
        @Override
        public void accept(Path path) {
            final File directory = new File(String.valueOf(path));
            if (directory.listFiles() != null && directory.listFiles().length == 0) {
                directory.delete();
            }
        }
    };

    private Consumer<Path> prepareForPurgeConsumer = new Consumer<Path>() {
        @Override
        public void accept(Path path) {
            if (path.endsWith(LifeTimeWatcher.FILE_NAME)) return;
            final File file = new File(String.valueOf(path));
            if (file.isFile()) {
                purgeSet.add(path);
            }
        }
    };

    public StorageSpaceInspector(long maxDiskSpace, String rootFolder) {
        this.maxDiskSpace = maxDiskSpace;
        this.rootFolder = rootFolder;

        evaluateUsedSpace();
    }

    private void evaluateUsedSpace() {
        usedSpace = 0;
        performInStorage(filesSizeConsumer);
    }

    public void clearEmptyDirectories() {
        performInStorage(deleteIfEmptyConsumer);
    }

    public void purge(long neededFreeSpace) {
        if (getFreeSpace() >= neededFreeSpace) return;

        purgeSet.clear();
        performInStorage(prepareForPurgeConsumer);

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
        try (final Stream<Path> walk = Files.walk(Paths.get(rootFolder))) {
            walk.forEach(consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            evaluateUsedSpace();
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

    public Consumer<Path> getFilesSizeConsumer() {
        return filesSizeConsumer;
    }

    public Consumer<Path> getDeleteIfEmptyConsumer() {
        return deleteIfEmptyConsumer;
    }
}
