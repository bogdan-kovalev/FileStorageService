package filestorage.impl;

import filestorage.impl.exception.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Bogdan Kovalev
 */
public class RealTest {
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
        final DefaultFileStorageService fileStorageService = new DefaultFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        final List<String> savedFiles = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (fileStorageService.getFreeStorageSpaceInPercents() > 0.1)
                        try {
                            final String key = getRandomFileName();
                            fileStorageService.saveFile(key, getRandomData());
                            savedFiles.add(key);
                            Thread.sleep(100);
                        } catch (FileAlreadyExistsException e) {
                            e.printStackTrace();
                        } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                            storageServiceIsNotStartedError.printStackTrace();
                        } catch (NotEnoughFreeSpaceException e) {
                            e.printStackTrace();
                        } catch (StorageCorruptedException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                    storageServiceIsNotStartedError.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                    if (!savedFiles.isEmpty())
                        try {
                            Thread.sleep(50);
                            try (InputStream inputStream = fileStorageService.readFile(savedFiles.get(random.nextInt(savedFiles.size())))) {
                            }
                        } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                            storageServiceIsNotStartedError.printStackTrace();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                    if (!savedFiles.isEmpty())
                        try {
                            Thread.sleep(70);
                            try (InputStream inputStream = fileStorageService.readFile(savedFiles.get(random.nextInt(savedFiles.size())))) {
                            }
                        } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                            storageServiceIsNotStartedError.printStackTrace();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                    if (!savedFiles.isEmpty())
                        try {
                            Thread.sleep(1000);
                            fileStorageService.deleteFile(savedFiles.get(random.nextInt(savedFiles.size())));
                        } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                            storageServiceIsNotStartedError.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                    if (!savedFiles.isEmpty())
                        try {
                            Thread.sleep(1000);
                            final int index = random.nextInt(savedFiles.size());
                            fileStorageService.deleteFile(savedFiles.get(index));
                            savedFiles.remove(index);
                        } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                            storageServiceIsNotStartedError.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (fileStorageService.getFreeStorageSpaceInPercents() > 0.1)
                        try {
                            Thread.sleep(70);
                            fileStorageService.saveFile(getRandomFileName(), getRandomData(), random.nextInt(20000));
                        } catch (FileAlreadyExistsException e) {
                            e.printStackTrace();
                        } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                            storageServiceIsNotStartedError.printStackTrace();
                        } catch (NotEnoughFreeSpaceException e) {
                            e.printStackTrace();
                        } catch (StorageCorruptedException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                    storageServiceIsNotStartedError.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (fileStorageService.getFreeStorageSpaceInPercents() > 0.1)
                        try {
                            Thread.sleep(30);
                            fileStorageService.saveFile(getRandomFileName(), getRandomData(), random.nextInt(20000));
                        } catch (FileAlreadyExistsException e) {
                            e.printStackTrace();
                        } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                            storageServiceIsNotStartedError.printStackTrace();
                        } catch (NotEnoughFreeSpaceException e) {
                            e.printStackTrace();
                        } catch (StorageCorruptedException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                    storageServiceIsNotStartedError.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(2000);
        while (!savedFiles.isEmpty())
            Thread.sleep(1000);

        fileStorageService.stopService();
    }
}
