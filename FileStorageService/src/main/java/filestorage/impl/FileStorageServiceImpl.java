package filestorage.impl;


import filestorage.FileStorageService;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

/**
 * @author Bogdan Kovalev
 */
public class FileStorageServiceImpl implements FileStorageService {

    private boolean serviceIsStarted = false;

    private final long maxDiskSpace;
    private final String rootFolder;

    private LiveTimeWatcher liveTimeWatcher;
    private StorageSpaceInspector storageSpaceInspector;
    private Thread storageSpaceInspectorThread;
    private Thread liveTimeWatcherThread;

    /**
     * @param maxDiskSpace max disk space can be used (in bytes)
     * @param rootFolder   root folder of file system
     */
    public FileStorageServiceImpl(long maxDiskSpace, String rootFolder) throws IOException {
        this.maxDiskSpace = maxDiskSpace;
        this.rootFolder = rootFolder;

        if (!createStorage()) {
            throw new IOException("Unable to create storage");
        }
    }

    public void startService() throws IOException {
        if (serviceIsStarted) {
            return;
        }
        storageSpaceInspector = new StorageSpaceInspector(maxDiskSpace, rootFolder);
        storageSpaceInspectorThread = new Thread(storageSpaceInspector);
        storageSpaceInspectorThread.start();

        liveTimeWatcher = new LiveTimeWatcher(rootFolder, storageSpaceInspector);
        if (liveTimeWatcher.getDataFileSize() > storageSpaceInspector.getFreeSpace()) {
            // TODO not enough free space exception
            throw new IOException("Not enough free space in " + rootFolder);
        }

        liveTimeWatcherThread = new Thread(liveTimeWatcher);
        liveTimeWatcherThread.start();
        serviceIsStarted = true;
    }

    public void stopService() {
        if (!serviceIsStarted) {
            return;
        }

        storageSpaceInspectorThread.interrupt();
        liveTimeWatcherThread.interrupt();

        storageSpaceInspector = null;
        liveTimeWatcher = null;

        storageSpaceInspectorThread = null;
        liveTimeWatcherThread = null;

        serviceIsStarted = false;
    }

    /**
     * This method provides possibility to saving data represented by {@code String} 'key' and {@code InputStream} 'inputStream'
     * into disk drive storage.
     * <p/>
     * Algorithm will save 'inputStream' into file in storage by using of the hashcode calculated for String 'key'.
     *
     * @param key         unique id
     * @param inputStream input stream
     * @throws IOException
     */
    @Override
    public void saveFile(String key, InputStream inputStream) throws IOException {
        if (!serviceIsStarted)
            //TODO throw ServiceIsNotStartedException
            throw new IllegalStateException();
        writeFile(getFilePath(key), Channels.newChannel(inputStream));
    }

    /**
     * This method provides possibility to saving data represented by {@code String} 'key' and {@code InputStream} 'inputStream'
     * into disk drive storage.
     * <p/>
     * Algorithm will save 'inputStream' into file in storage by using of the hashcode calculated for String 'key'. File
     * will be deleted after 'liveTimeMillis' milliseconds.
     *
     * @param key            unique id
     * @param inputStream    input stream
     * @param liveTimeMillis live time of the stored file
     * @throws IOException
     */
    @Override
    public void saveFile(String key, InputStream inputStream, long liveTimeMillis) throws IOException {
        if (!serviceIsStarted)
            //TODO throw ServiceIsNotStartedException
            throw new IllegalStateException();
        final String filePath = getFilePath(key);
        writeFile(filePath, Channels.newChannel(inputStream));
        liveTimeWatcher.addFile(filePath, liveTimeMillis);
    }

    @Override
    public InputStream readFile(String key) {
        if (!serviceIsStarted)
            //TODO throw ServiceIsNotStartedException
            throw new IllegalStateException();
        try {
            return Channels.newInputStream(new FileInputStream(getFilePath(key)).getChannel());
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public void deleteFile(String key) {
        if (!serviceIsStarted)
            //TODO throw ServiceIsNotStartedException
            throw new IllegalStateException();
        final File file = new File(getFilePath(key));
        try {
            final long length = file.length();
            if (Files.deleteIfExists(file.toPath()))
                storageSpaceInspector.incrementFreeSpace(length);
        } catch (IOException e) {
            //TODO файл не может быть удален
        }
    }

    /**
     * @return free storage space in bytes
     */
    @Override
    public long getFreeStorageSpace() {
        if (!serviceIsStarted)
            //TODO throw ServiceIsNotStartedException
            throw new IllegalStateException();
        return storageSpaceInspector.getFreeSpace();
    }

    @Override
    public void purge(double percents) {
        if (!serviceIsStarted)
            //TODO throw ServiceIsNotStartedException
            throw new IllegalStateException();
        throw new IllegalStateException("Not implemented yet");
    }

    public long getWorkingDataSize() throws IOException {
        if (!serviceIsStarted)
            //TODO throw ServiceIsNotStartedException
            throw new IllegalStateException();
        return liveTimeWatcher.getDataFileSize();
    }

    private void writeFile(String filePath, ReadableByteChannel channel) throws IOException {
        try (final FileChannel out = new FileOutputStream(filePath).getChannel()) {
            final ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (channel.read(buffer) != -1) {
                if (!haveEnoughFreeSpace()) {
                    // TODO файл не дописан до конца, значит его нужно удалить. Кинуть соответствующий эксепшн
                    throw new IOException("Not enough free space in " + rootFolder);
                }
                buffer.flip();
                out.write(buffer);

                storageSpaceInspector.decrementFreeSpace(buffer.position());

                buffer.clear();
            }
        }
    }

    private boolean haveEnoughFreeSpace() {
        return getFreeStorageSpace() > 1024;
    }

    private String getFilePath(String key) {
        return PathConstructor.constructFilePathInStorage(key, rootFolder);
    }

    private boolean createStorage() {
        final File root = new File(rootFolder);
        return root.exists() || root.mkdir();
    }
}
