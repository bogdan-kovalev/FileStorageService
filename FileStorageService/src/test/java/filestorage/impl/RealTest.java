package filestorage.impl;

import filestorage.impl.exception.StorageServiceIsNotStartedError;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Bogdan Kovalev.
 */
public class RealTest {
    private static final String STORAGE_ROOT = "storage";
    private static final int MAX_DISK_SPACE = 1024000;
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
        return new ByteArrayInputStream(new byte[random.nextInt(40000) + 10000]);
    }

    @Test
    public void multiThreadingTest() throws Exception {
        final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();
        Thread.sleep(500);

        final List<String> savedFiles = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (fileStorageService.serviceIsStarted() & fileStorageService.getFreeStorageSpace() > MAX_DISK_SPACE * 0.1) {
                        try {
                            final String fileName = getRandomFileName();
                            System.out.println("saveFile( " + fileName + " )");
                            fileStorageService.saveFile(fileName, getRandomData());
                            savedFiles.add(fileName);
                            Thread.sleep(50);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println();
                    }
                } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                    storageServiceIsNotStartedError.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (fileStorageService.serviceIsStarted()) {
                    try {
                        Thread.sleep(50);
                        if (savedFiles.isEmpty()) continue;
                        final String key = savedFiles.get(random.nextInt(savedFiles.size()));
                        System.out.println("readFile( " + key + ")");
                        try (InputStream inputStream = fileStorageService.readFile(key)) {
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (fileStorageService.serviceIsStarted()) {
                    try {
                        Thread.sleep(200);
                        if (savedFiles.isEmpty()) continue;
                        final int index = random.nextInt(savedFiles.size());
                        final String key = savedFiles.get(index);
                        System.out.println("deleteFile( " + key + " )");
                        fileStorageService.deleteFile(key);
                        savedFiles.remove(index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    while (fileStorageService.serviceIsStarted() & fileStorageService.getFreeStorageSpace() > MAX_DISK_SPACE * 0.1) {
                        try {
                            final int liveTimeMillis = random.nextInt(2000) + 500;
                            final String fileName = getRandomFileName();
                            System.out.println("saveFile( " + fileName + " , " + liveTimeMillis + " )");
                            fileStorageService.saveFile(fileName, getRandomData(), liveTimeMillis);
                            Thread.sleep(30);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println();
                    }
                } catch (StorageServiceIsNotStartedError storageServiceIsNotStartedError) {
                    storageServiceIsNotStartedError.printStackTrace();
                }
            }
        }).start();
        Thread.sleep(500);

        while (!savedFiles.isEmpty())
            Thread.sleep(5);

        fileStorageService.stopService();
        System.out.println("Service stopped!!!");
    }
}
