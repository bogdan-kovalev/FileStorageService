package filestorage.impl;

import filestorage.impl.exception.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertFalse;

/**
 * @author Bogdan Kovalev
 */
public class RealTest {

    private static final Logger LOG = LoggerFactory.getLogger(RealTest.class);

    private static final String STORAGE_ROOT = "storage";
    private static final int MAX_DISK_SPACE = 10240;
    private static Random random = new Random();

    private static String getRandomFileName() {
        int nameLength = random.nextInt(5) + 10;
        StringBuilder fileName = new StringBuilder();
        while (nameLength-- > 0) {
            final char randomChar = (char) (random.nextInt('z' - 'a') + 'a');
            fileName.append(randomChar);
        }
        return fileName.toString();
    }

    private static ByteArrayInputStream getRandomData() {
        return new ByteArrayInputStream(new byte[random.nextInt(400) + 100]);
    }

    @Test
    public void testMultiThreading() throws UnableToCreateStorageException, ServiceStartError, InterruptedException {
        LOG.info("############  testMultiThreading() ############\n");
        final DefaultFileStorageService fileStorageService = new DefaultFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        final List<String> savedFiles = new ArrayList<>();
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (fileStorageService.getFreeStorageSpaceInPercents() > 0.1) {
                            final String key = getRandomFileName();
                            fileStorageService.saveFile(key, getRandomData());
                            savedFiles.add(key);
                            Thread.sleep(100);
                        }
                    } catch (FileAlreadyExistsException | InterruptedException | NotEnoughFreeSpaceException e) {
                        LOG.info(e.toString());
                    } catch (StorageCorruptedException | StorageServiceIsNotStartedError e) {
                        LOG.error(e.toString());
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true)
                        try {
                            Thread.sleep(500);
                            final int index = savedFiles.isEmpty() ? -1 : random.nextInt(savedFiles.size());
                            if (index < 0) continue;
                            fileStorageService.saveFile(savedFiles.get(index), getRandomData());
                            assertFalse("Saved already existed file", true);
                        } catch (FileAlreadyExistsException | InterruptedException | NotEnoughFreeSpaceException e) {
                            LOG.info(e.toString());
                        } catch (StorageCorruptedException | StorageServiceIsNotStartedError e) {
                            LOG.error(e.toString());
                        }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        final int index = savedFiles.isEmpty() ? -1 : random.nextInt(savedFiles.size());
                        if (index < 0) continue;

                        try (InputStream inputStream = fileStorageService.readFile(savedFiles.get(index))) {
                            while (inputStream.read() != -1) {
                                Thread.sleep(1);
                            }
                        } catch (FileNotFoundException e) {
                            LOG.info(e.toString());
                        } catch (StorageServiceIsNotStartedError | IOException | InterruptedException e) {
                            LOG.error(e.toString());
                        }

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            LOG.error(e.getMessage());
                        }
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true)
                        try {
                            Thread.sleep(100);
                            final int index = savedFiles.isEmpty() ? -1 : random.nextInt(savedFiles.size());
                            if (index < 0) continue;
                            fileStorageService.deleteFile(savedFiles.get(index));
                            savedFiles.remove(index);
                        } catch (InterruptedException | MaybeFileInUseException e) {
                            LOG.info(e.toString());
                        } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                            LOG.error(storageServiceIsNotStartedError.toString());
                        }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (fileStorageService.getFreeStorageSpaceInPercents() > 0.1) {
                            Thread.sleep(5);
                            fileStorageService.saveFile(getRandomFileName(), getRandomData(), random.nextInt(500));
                        }
                    } catch (FileAlreadyExistsException | InterruptedException | NotEnoughFreeSpaceException e) {
                        LOG.info(e.toString());
                    } catch (StorageCorruptedException | StorageServiceIsNotStartedError e) {
                        LOG.error(e.toString());
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (fileStorageService.getFreeStorageSpaceInPercents() > 0.1) {
                            Thread.sleep(7);
                            fileStorageService.saveFile(getRandomFileName(), getRandomData(), random.nextInt(500));
                        }
                    } catch (FileAlreadyExistsException | InterruptedException | NotEnoughFreeSpaceException e) {
                        LOG.info(e.toString());
                    } catch (StorageCorruptedException | StorageServiceIsNotStartedError e) {
                        LOG.error(e.toString());
                    }
                }
            }).start();

            Thread.sleep(500);
            while (!savedFiles.isEmpty())
                Thread.sleep(1000);

            fileStorageService.stopService();

        } catch (IllegalStateException e) {
            assertFalse(true);
        }
    }
}
