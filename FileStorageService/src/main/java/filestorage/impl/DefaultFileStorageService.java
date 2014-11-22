package filestorage.impl;

import filestorage.FileStorageService;
import filestorage.impl.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default implementation of FileStorageService.
 *
 * @author Bogdan Kovalev
 */
public class DefaultFileStorageService implements FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultFileStorageService.class);

    public static final String SYSTEM_FOLDER_NAME = "system";
    public static final String SYSTEM_FILE_NAME = "system.data";
    public static final String DATA_FOLDER_NAME = "data";

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
    public DefaultFileStorageService(long diskSpace, String storageRoot) throws UnableToCreateStorageException {
        this.diskSpace = diskSpace;
        this.STORAGE_ROOT = storageRoot;

        LOG.info("New storage. Disk space: {}. Storage root: {}", diskSpace, storageRoot);

        this.dataFolderPath = String.valueOf(Paths.get(STORAGE_ROOT, DATA_FOLDER_NAME));

        if (!createStorage()) {
            throw new UnableToCreateStorageException();
        }

        LOG.info("Storage created!");
    }

    /**
     * This method starts all accessorial modules of the service.
     * If service is not started, all public methods will be throw {@code StorageServiceIsNotStartedError}.
     *
     * @throws ServiceStartError
     */
    public void startService() throws ServiceStartError {
        LOG.info("Try to start the service...");
        if (serviceIsStarted) {
            LOG.info("Service is already started");
            return;
        }

        storageSpaceInspector = new StorageSpaceInspector(diskSpace, STORAGE_ROOT);

        try {
            lifeTimeWatcher = new LifeTimeWatcher(STORAGE_ROOT, storageSpaceInspector);
        } catch (IOException e) {
            LOG.error("Service start is failed.");
            throw new ServiceStartError();
        }

        lifeTimeWatcherThread = new Thread(lifeTimeWatcher);
        lifeTimeWatcherThread.start();

        serviceIsStarted = true;

        LOG.info("Service is started successfully.");
    }

    /**
     * This method stops all accessorial modules.
     * After stopping, all public methods of the service will be throw {@code StorageServiceIsNotStartedError}.
     */
    public void stopService() {
        LOG.info("Try to stop the service...");
        if (!serviceIsStarted) {
            LOG.info("Service is not started");
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
        LOG.info("Service is stopped.");
    }

    @Override
    public void saveFile(String key, InputStream inputStream) throws FileAlreadyExistsException, StorageServiceIsNotStartedError, NotEnoughFreeSpaceException, StorageCorruptedException, FileLockedException {
        LOG.info("Saving of '{}' ...", key);
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        final String destinationPath = PathConstructor.calculateDestinationPath(key, dataFolderPath);

        Path filePath = Paths.get(destinationPath, key);

        try {
            Files.createDirectories(Paths.get(destinationPath));
            Files.createFile(filePath);
        } catch (FileAlreadyExistsException e) {
            LOG.warn(e.getMessage());
            throw new FileAlreadyExistsException(key);
        } catch (IOException e) {
            LOG.error("Can't create a file {}", key);
            throw new IllegalStateException(e.getMessage());
        }

        try {
            writeFile(filePath, Channels.newChannel(inputStream));
        } catch (IOException e) {
            LOG.error("Can't write to a file: '{}'", key);
            throw new IllegalStateException(e.getMessage());
        }

        LOG.info("File '{}' saved", key);
    }

    @Override
    public void saveFile(String key, InputStream inputStream, long lifeTimeMillis) throws FileAlreadyExistsException, StorageServiceIsNotStartedError, NotEnoughFreeSpaceException, FileLockedException, StorageCorruptedException {
        LOG.info("Life-time of '{}' = {} milliseconds", key, lifeTimeMillis);
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        saveFile(key, inputStream);

        lifeTimeWatcher.addFile(key, lifeTimeMillis);
    }

    @Override
    public InputStream readFile(String key) throws StorageServiceIsNotStartedError, FileNotFoundException {
        LOG.info("Reading of '{}' ...", key);
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        Path filePath = Paths.get(PathConstructor.calculateDestinationPath(key, dataFolderPath), key);

        return new BufferedInputStream(new FileInputStream(String.valueOf(filePath)));
    }

    @Override
    public void deleteFile(String key) throws StorageServiceIsNotStartedError {
        LOG.info("Deleting of '{}' ...", key);
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        Path filePath = Paths.get(PathConstructor.calculateDestinationPath(key, dataFolderPath), key);

        if (Files.exists(filePath)) {
            try {
                final long length = Files.size(filePath);
                Files.delete(filePath);
                storageSpaceInspector.decrementUsedSpace(length);
            } catch (IOException e) {
                LOG.error("Can't delete file {}", key);
                throw new IllegalStateException(e.getMessage());
            }
        }
        LOG.info("File '{}' deleted.", key);
    }

    @Override
    public long getFreeStorageSpaceInBytes() throws StorageServiceIsNotStartedError {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();
        return storageSpaceInspector.getFreeSpace();
    }

    @Override
    public float getFreeStorageSpaceInPercents() throws StorageServiceIsNotStartedError {
        return getFreeStorageSpaceInBytes() / diskSpace;
    }

    /**
     * This method releases free disk space by deleting old files.
     *
     * @param neededFreeSpaceInPercents - The percentage of required free space from the total disk space.
     * @throws StorageServiceIsNotStartedError
     * @throws InvalidPercentsValueException
     */
    @Override
    public void purge(float neededFreeSpaceInPercents) throws StorageServiceIsNotStartedError, InvalidPercentsValueException {
        if (neededFreeSpaceInPercents < 0 || neededFreeSpaceInPercents > 1)
            throw new InvalidPercentsValueException();

        purge((long) (diskSpace * neededFreeSpaceInPercents));
    }

    @Override
    public void purge(long neededFreeSpaceInBytes) throws StorageServiceIsNotStartedError {
        LOG.info("Start purging of a storage disk space.");
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();

        storageSpaceInspector.purge(neededFreeSpaceInBytes);
        LOG.info("{} bytes of the storage disk space was successfully purged.", neededFreeSpaceInBytes);
    }

    public long getSystemFolderSize() throws StorageServiceIsNotStartedError {
        if (!serviceIsStarted)
            throw new StorageServiceIsNotStartedError();
        return storageSpaceInspector.getSystemFolderSize();
    }

    public void deleteEmptyDirectories() {
        LOG.info("Deleting of an empty directories.");
        storageSpaceInspector.deleteEmptyDirectories(new File(dataFolderPath));
        LOG.info("Empty directories are deleted");
    }

    public boolean serviceIsStarted() {
        return serviceIsStarted;
    }

    private void writeFile(Path filePath, ReadableByteChannel channel) throws IOException, StorageCorruptedException, NotEnoughFreeSpaceException, StorageServiceIsNotStartedError, FileLockedException {
        LOG.info("Writing of '{}' onto a disk space...", filePath);

        try (final FileChannel out = new FileOutputStream(String.valueOf(filePath)).getChannel()) {

            final ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (channel.read(buffer) != -1) {
                if (getFreeStorageSpaceInBytes() < 1024) {
                    throw new NotEnoughFreeSpaceException();
                }
                buffer.flip();
                out.write(buffer);

                storageSpaceInspector.incrementUsedSpace(buffer.position());

                buffer.clear();
            }
        } catch (FileNotFoundException e) {
            // arises when destination folders hierarchy corrupted
            throw new StorageCorruptedException();
        }
        LOG.info("'{}' successfully written", filePath);
    }

    /**
     * @return true if and only if storage root folder was created.
     */
    private boolean createStorage() {
        final File root = new File(STORAGE_ROOT);
        return root.exists() || root.mkdir();
    }
}
