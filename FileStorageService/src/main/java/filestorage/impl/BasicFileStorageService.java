package filestorage.impl;

import filestorage.FileStorageService;
import filestorage.StorageException;
import filestorage.impl.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Basic realization of FileStorageService.
 *
 * @author Bogdan Kovalev
 */
public class BasicFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(BasicFileStorageService.class);

    public static final String SYSTEM_FOLDER_NAME = "system";
    public static final String SYSTEM_FILE_NAME = "system.data";
    public static final String DATA_FOLDER_NAME = "data";
    private static final int BUFFER_SIZE = 1024;

    private final String STORAGE_ROOT;

    private boolean serviceIsStarted = false;

    private final long diskSpace;
    private final String dataFolderPath;

    private LifeTimeWatcher lifeTimeWatcher;
    private StorageSpaceInspector storageSpaceInspector;
    private Thread lifeTimeWatcherThread;

    /**
     * @param diskSpace   Maximum disk space that service can use for work
     * @param storageRoot String path to storage root folder
     * @throws UnableToCreateStorageException
     */
    public BasicFileStorageService(long diskSpace, String storageRoot) throws UnableToCreateStorageException {
        this.diskSpace = diskSpace;
        this.STORAGE_ROOT = storageRoot;

        logger.info("New storage. Disk space: {}. Storage root: {}", diskSpace, storageRoot);

        this.dataFolderPath = String.valueOf(Paths.get(STORAGE_ROOT, DATA_FOLDER_NAME));

        if (!createStorage()) {
            throw new UnableToCreateStorageException();
        }

        logger.info("Storage created!");
    }

    /**
     * This method starts all accessorial modules of the service.
     * If service is not started, all public methods will be throw {@code StorageServiceIsNotStartedError}.
     *
     * @throws ServiceStartError
     */
    public void startService() throws ServiceStartError {
        logger.info("Try to start the service...");
        if (serviceIsStarted) {
            logger.info("Service is already started");
            return;
        }

        storageSpaceInspector = new StorageSpaceInspector(diskSpace, STORAGE_ROOT);

        try {
            lifeTimeWatcher = new LifeTimeWatcher(STORAGE_ROOT, storageSpaceInspector);
        } catch (IOException e) {
            throw new ServiceStartError();
        }

        lifeTimeWatcherThread = new Thread(lifeTimeWatcher);
        lifeTimeWatcherThread.start();

        serviceIsStarted = true;

        logger.info("Service is started successfully.");
    }

    /**
     * This method stops all accessorial modules.
     * After stopping, all public methods of the service will be throw {@code StorageServiceIsNotStartedError}.
     */
    public void stopService() {
        logger.info("Try to stop the service...");
        if (!serviceIsStarted) {
            logger.info("Service is not started");
            return;
        }
        lifeTimeWatcherThread.interrupt();

        synchronized (lifeTimeWatcher) {
            try {
                lifeTimeWatcher.wait(1000);
            } catch (InterruptedException ignored) {
            }
        }

        lifeTimeWatcher = null;
        lifeTimeWatcherThread = null;

        serviceIsStarted = false;
        logger.info("Service is stopped.");
    }

    @Override
    public void saveFile(String key, InputStream inputStream) throws StorageException, IOException {
        logger.info("Saving of '{}' ...", key);
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        final String destinationPath = PathConstructor.calculateDestinationPath(key, dataFolderPath);

        Path filePath = Paths.get(destinationPath, key);
        if (Files.exists(filePath))
            throw new FileAlreadyExistsException(key);

        Files.createDirectories(Paths.get(destinationPath));
        writeFile(filePath, Channels.newChannel(inputStream));
        logger.info("File '{}' saved", key);
    }

    @Override
    public void saveFile(String key, InputStream inputStream, long lifeTimeMillis) throws StorageException, IOException {
        logger.info("Life-time of '{}' = {} milliseconds", key, lifeTimeMillis);
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        saveFile(key, inputStream);

        lifeTimeWatcher.addFile(key, lifeTimeMillis);
    }

    @Override
    public InputStream readFile(String key) throws StorageServiceIsNotStartedError, FileNotFoundException {
        logger.info("Reading of '{}' ...", key);
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        Path filePath = Paths.get(PathConstructor.calculateDestinationPath(key, dataFolderPath), key);

        return new BufferedInputStream(new FileInputStream(String.valueOf(filePath)));
    }

    @Override
    public void deleteFile(String key) throws StorageServiceIsNotStartedError, IOException {
        logger.info("Deleting of '{}' ...", key);
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        Path filePath = Paths.get(PathConstructor.calculateDestinationPath(key, dataFolderPath), key);

        if (Files.exists(filePath)) {
            final long length = Files.size(filePath);
            Files.delete(filePath);
            storageSpaceInspector.decrementUsedSpace(length);
        }
        logger.info("File '{}' deleted.", key);
    }

    @Override
    public long getFreeStorageSpace() throws StorageServiceIsNotStartedError {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();
        return storageSpaceInspector.getFreeSpace();
    }

    /**
     * This method releases free disk space by deleting old files.
     *
     * @param percents - The percentage of required free space from the total disk space.
     * @throws StorageServiceIsNotStartedError
     * @throws InvalidPercentsValueException
     */
    @Override
    public void purge(float percents) throws StorageServiceIsNotStartedError, InvalidPercentsValueException {
        logger.info("Start purging of the storage disk space.");
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        if (percents < 0 || percents > 1)
            throw new InvalidPercentsValueException();

        storageSpaceInspector.purge((long) (diskSpace * percents));
        logger.info("{} percents of the storage disk space was successfully purged.", percents);
    }

    public long getSystemFolderSize() throws StorageServiceIsNotStartedError {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();
        return storageSpaceInspector.getSystemFolderSize();
    }

    public void deleteEmptyDirectories() {
        logger.info("Deleting of empty directories.");
        storageSpaceInspector.deleteEmptyDirectories(new File(dataFolderPath));
        logger.info("Empty directories are deleted");
    }

    public boolean serviceIsStarted() {
        return serviceIsStarted;
    }

    private void writeFile(Path filePath, ReadableByteChannel channel) throws IOException, StorageException {
        logger.info("Writing of '{}' onto the disk space...", filePath);
        FileLock fileLock = null;
        try (final FileChannel out = new FileOutputStream(String.valueOf(filePath)).getChannel()) {
            try {
                fileLock = tryToLock(out);

                final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

                while (channel.read(buffer) != -1) {
                    if (getFreeStorageSpace() < BUFFER_SIZE) {
                        throw new NotEnoughFreeSpaceException();
                    }
                    buffer.flip();
                    out.write(buffer);

                    storageSpaceInspector.incrementUsedSpace(buffer.position());

                    buffer.clear();
                }
            } finally {
                if (fileLock != null) {
                    fileLock.release();
                }
            }
        } catch (FileNotFoundException e) {
            // arises when destination folders hierarchy corrupted
            throw new StorageCorruptedException();
        }
        logger.info("'{}' successfully wrote", filePath);
    }

    /**
     * This method tries to lock current FileChannel and throw {@code FileLockedException} if it already locked.
     *
     * @param out current FileChannel
     * @return FileLock
     * @throws FileLockedException
     */
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

    /**
     * @return true if and only if storage root folder was created.
     */
    private boolean createStorage() {
        final File root = new File(STORAGE_ROOT);
        return root.exists() || root.mkdir();
    }
}
