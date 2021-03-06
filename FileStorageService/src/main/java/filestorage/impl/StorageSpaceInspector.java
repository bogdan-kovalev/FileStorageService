package filestorage.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.TreeSet;

/**
 * This class provides possibility to calculate used storage disk space, purge files, calculate system folder size and
 * delete empty directories.
 *
 * @author Bogdan Kovalev.
 */
public class StorageSpaceInspector {

    private static final Logger LOG = LoggerFactory.getLogger(StorageSpaceInspector.class);

    private final long diskSpace;
    private final String STORAGE_ROOT;

    private long usedSpace;

    private TreeSet<Path> purgeSet = new TreeSet<>(new Comparator<Path>() {
        @Override
        public int compare(Path file1, Path file2) {
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

    public StorageSpaceInspector(long diskSpace, String STORAGE_ROOT) {
        this.diskSpace = diskSpace;
        this.STORAGE_ROOT = STORAGE_ROOT;

        evaluateUsedSpace();
    }

    private void evaluateUsedSpace() {
        usedSpace = 0;
        performInStorage(incrementUsedSpace);
    }

    public void deleteEmptyDirectories(File start) {
        final File[] files = start.listFiles();

        if (files == null) return;

        for (File file : files) {
            deleteEmptyDirectories(file);
        }

        if (start.listFiles().length == 0) {
            if (start.delete())
                if (LOG.isInfoEnabled())
                    LOG.info("'{}' directory deleted!", start);
        }
    }

    /**
     * This method releases free disk space by deleting old files.
     *
     * @param neededFreeSpace in bytes.
     */
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

    /**
     * This method will be accept the consumer for all file-paths in the storage.
     *
     * @param consumer consumer with specific function
     */
    private void performInStorage(Consumer consumer) {
        Deque<File> stack = new ArrayDeque<>();
        stack.push(new File(STORAGE_ROOT));

        while (!stack.isEmpty()) {
            final File[] files = stack.pop().listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (file.isDirectory()) {
                    stack.push(file);
                    continue;
                }
                consumer.accept(file.toPath());
            }
        }
    }

    public long getFreeSpace() {
        return diskSpace - usedSpace;
    }

    public void incrementUsedSpace(long bytes) {
        usedSpace += bytes;
    }

    public void decrementUsedSpace(long bytes) {
        usedSpace -= bytes;
    }

    public long getSystemFolderSize() {
        long size = 0;
        String systemFolderPath = String.valueOf(Paths.get(STORAGE_ROOT, DefaultFileStorageService.SYSTEM_FOLDER_NAME));

        final File[] files = new File(systemFolderPath).listFiles();
        if (files != null)
            for (File file : files) {
                size += file.length();
            }
        return size;
    }

    private Consumer incrementUsedSpace = new Consumer() {
        @Override
        public void accept(Path path) {
            final File file = new File(String.valueOf(path));
            if (file.isFile())
                usedSpace += file.length();
        }
    };

    private Consumer prepareForPurge = new Consumer() {
        @Override
        public void accept(Path path) {
            if (path.endsWith(DefaultFileStorageService.SYSTEM_FILE_NAME)) return;
            final File file = new File(String.valueOf(path));
            if (file.isFile()) {
                purgeSet.add(path);
            }
        }
    };

    static interface Consumer {
        public void accept(Path path);
    }
}
