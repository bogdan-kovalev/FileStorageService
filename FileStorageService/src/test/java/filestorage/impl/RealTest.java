package filestorage.impl;

import filestorage.impl.exception.ServiceStartError;
import filestorage.impl.exception.UnableToCreateStorageException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertFalse;

/**
 * @author Bogdan Kovalev
 */
public class RealTest {
    private static final String STORAGE_ROOT = "storage";
    private static final int MAX_DISK_SPACE = 102400;
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true)
                        try {
                            Thread.sleep(50);
                            final int index = savedFiles.isEmpty() ? -1 : random.nextInt(savedFiles.size());
                            if (index < 0) continue;
                            try (InputStream inputStream = fileStorageService.readFile(savedFiles.get(index))) {
                                while (inputStream.read() != -1) {
                                    Thread.sleep(1);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true)
                        try {
                            Thread.sleep(70);
                            final int index = savedFiles.isEmpty() ? -1 : random.nextInt(savedFiles.size());
                            if (index < 0) continue;
                            try (InputStream inputStream = fileStorageService.readFile(savedFiles.get(index))) {
                                while (inputStream.read() != -1) {
                                    Thread.sleep(1);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true)
                        try {
                            Thread.sleep(300);
                            final int index = savedFiles.isEmpty() ? -1 : random.nextInt(savedFiles.size());
                            if (index < 0) continue;
                            fileStorageService.deleteFile(savedFiles.get(index));
                        } catch (Exception e) {
                            e.printStackTrace();
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
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (fileStorageService.getFreeStorageSpaceInPercents() > 0.1) {
                            Thread.sleep(1);
                            fileStorageService.saveFile(getRandomFileName(), getRandomData(), random.nextInt(20000));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (fileStorageService.getFreeStorageSpaceInPercents() > 0.1) {
                            Thread.sleep(1);
                            fileStorageService.saveFile(getRandomFileName(), getRandomData(), random.nextInt(20000));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            Thread.sleep(2000);
            while (!savedFiles.isEmpty())
                Thread.sleep(1000);

            fileStorageService.stopService();

        } catch (IllegalStateException e) {
            assertFalse(true);
        }
    }
}
