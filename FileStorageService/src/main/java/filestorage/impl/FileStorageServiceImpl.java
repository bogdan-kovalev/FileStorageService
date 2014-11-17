package filestorage.impl;


import filestorage.FileStorageService;
import filestorage.impl.exception.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Bogdan Kovalev
 */
public class FileStorageServiceImpl implements FileStorageService {

    private boolean serviceIsStarted = false;

    private final long maxDiskSpace;
    private final String rootFolder;

    private LifeTimeWatcher lifeTimeWatcher;
    private StorageSpaceInspector storageSpaceInspector;
    private Thread storageSpaceInspectorThread;
    private Thread liveTimeWatcherThread;

    /**
     * @param maxDiskSpace max disk space can be used (in bytes)
     * @param rootFolder   root folder of file system
     */
    public FileStorageServiceImpl(long maxDiskSpace, String rootFolder) throws UnableToCreateStorageException {
        this.maxDiskSpace = maxDiskSpace;
        this.rootFolder = rootFolder;

        if (!createStorage()) {
            throw new UnableToCreateStorageException();
        }
    }

    public void startService() throws NotEnoughFreeSpaceException, LifeTimeWatcherStartError {
        if (serviceIsStarted) {
            return;
        }
        storageSpaceInspector = new StorageSpaceInspector(maxDiskSpace, rootFolder);
        storageSpaceInspectorThread = new Thread(storageSpaceInspector);
        storageSpaceInspectorThread.start();

        try {
            lifeTimeWatcher = new LifeTimeWatcher(rootFolder, storageSpaceInspector);
        } catch (IOException e) {
            throw new LifeTimeWatcherStartError();
        }
        if (lifeTimeWatcher.getDataFileSize() > storageSpaceInspector.getFreeSpace()) {
            throw new NotEnoughFreeSpaceException();
        }

        liveTimeWatcherThread = new Thread(lifeTimeWatcher);
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
        lifeTimeWatcher = null;

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
    public void saveFile(String key, InputStream inputStream) throws FileLockedException, NotEnoughFreeSpaceException, StorageServiceIsNotStartedError, StorageCorruptedException, FileAlreadyExistsException {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        final String destinationPath = PathConstructor.findDestinationPath(key, rootFolder);
        Path filePath = Paths.get(destinationPath, key);
        if (Files.exists(filePath))
            throw new FileAlreadyExistsException(key);

        try {
            Files.createDirectories(Paths.get(destinationPath));
            writeFile(filePath, Channels.newChannel(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public void saveFile(String key, InputStream inputStream, long liveTimeMillis) throws FileLockedException, NotEnoughFreeSpaceException, StorageServiceIsNotStartedError, IOException, StorageCorruptedException {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        saveFile(key, inputStream);

        lifeTimeWatcher.addFile(key, liveTimeMillis);
    }

    @Override
    public InputStream readFile(String key) throws StorageServiceIsNotStartedError, FileNotFoundException {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        Path filePath = Paths.get(PathConstructor.findDestinationPath(key, rootFolder), key);


        return Channels.newInputStream(new FileInputStream(String.valueOf(filePath)).getChannel());
    }

    @Override
    public void deleteFile(String key) throws StorageServiceIsNotStartedError {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        Path filePath = Paths.get(PathConstructor.findDestinationPath(key, rootFolder), key);
        if (Files.exists(filePath)) {
            try {
                final long length = Files.size(filePath);
                Files.delete(filePath);
                storageSpaceInspector.decrementUsedSpace(length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return free storage space in bytes
     */
    @Override
    public long getFreeStorageSpace() throws StorageServiceIsNotStartedError {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        return storageSpaceInspector.getFreeSpace();
    }

    @Override
    public void purge(float percents) throws StorageServiceIsNotStartedError, InvalidPercentsValueException {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        if (percents >= 0 & percents <= 1)
            storageSpaceInspector.purge((long) (maxDiskSpace * percents));
        else
            throw new InvalidPercentsValueException();
    }

    public long getWorkingDataSize() throws StorageServiceIsNotStartedError {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        return storageSpaceInspector.getDataFolderSize();
    }

    private void writeFile(Path filePath, ReadableByteChannel channel) throws NotEnoughFreeSpaceException, StorageServiceIsNotStartedError, StorageCorruptedException, IOException, FileLockedException {
        FileLock fileLock = null;
        try (final FileChannel out = new FileOutputStream(String.valueOf(filePath)).getChannel()) {

            try {
                fileLock = tryToLock(out);

                final ByteBuffer buffer = ByteBuffer.allocate(1024);

                while (channel.read(buffer) != -1) {
                    if (getFreeStorageSpace() < 1024) {
                        throw new NotEnoughFreeSpaceException();
                    }
                    buffer.flip();
                    out.write(buffer);

                    storageSpaceInspector.incrementUsedSpace(buffer.position());

                    buffer.clear();
                }
            } catch (NotEnoughFreeSpaceException e) {
                throw e;
            } finally {
                if (fileLock != null) {
                    fileLock.release();
                }
            }
        } catch (FileNotFoundException e) {
            throw new StorageCorruptedException();
        } catch (FileLockedException e) {
            throw e;
        }
    }

    private FileLock tryToLock(FileChannel out) throws FileLockedException {
        FileLock lock = null;
        try {
            lock = out.lock();
        } catch (OverlappingFileLockException e) {
            throw new FileLockedException();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lock;
    }

    private boolean createStorage() {
        final File root = new File(rootFolder);
        return root.exists() || root.mkdir();
    }
}
