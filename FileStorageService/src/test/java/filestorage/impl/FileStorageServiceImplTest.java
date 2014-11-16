package filestorage.impl;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class FileStorageServiceImplTest {

    public static final String STORAGE_PATH = ".".concat(File.separator).concat("storage");

    @Test
    public void testSaveFiles() throws Exception {
        final int maxDiskSpace = 2000000;
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        String file1_name = "file1_testSaveFiles";
        String file2_name = "file2_testSaveFiles";
        String file3_name = "file3_testSaveFiles";

        fileStorageService.saveFile(file1_name, new ByteArrayInputStream(new byte[]{}));
        fileStorageService.saveFile(file2_name, new ByteArrayInputStream(new byte[]{}));
        fileStorageService.saveFile(file3_name, new ByteArrayInputStream(new byte[]{}));

        assertTrue(fileStorageService.readFile(file1_name) != null);
        assertTrue(fileStorageService.readFile(file2_name) != null);
        assertTrue(fileStorageService.readFile(file3_name) != null);
    }

    @Test
    public void testReadFile() throws IOException {
        final int maxDiskSpace = 2000000;
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        String filename = "testReadFile";
        final byte testByte = 77;
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{testByte}));

        final InputStream file = fileStorageService.readFile(filename);

        assertTrue(file.read() == testByte);
    }

    @Test
    public void testSaveFileWithLiveTime() throws IOException, InterruptedException {
        final int maxDiskSpace = 2000000;
        final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        final int liveTimeMillis = 2000;
        final String filename = "fileWithLiveTime";
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{}), liveTimeMillis);

        Thread.sleep(liveTimeMillis + 100);

        assertTrue(fileStorageService.readFile(filename) == null);
    }

    @Test
    public void testDeleteFile() throws IOException {
        final int maxDiskSpace = 2000000;
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        String filename = "testDeleteFile";
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{}));
        fileStorageService.deleteFile(filename);

        assertTrue(fileStorageService.readFile(filename) == null);
    }

    @Test
    public void testStorageNotEnoughSpaceForCreateStorage() {
        final String filename = "testStorageSize";
        IOException ioException = null;
        try {
            final int maxDiskSpace = 5;
            final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
            fileStorageService.startService();
            fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{}));
        } catch (IOException e) {
            ioException = e;
            e.printStackTrace();
        }
        assertTrue(ioException != null && ioException.getMessage().equals("Not enough free space in " + STORAGE_PATH));
    }

    @Test
    public void testStorageNotEnoughSpaceForFileSave() {
        final String filename = "testStorageSize";
        IOException ioException = null;
        try {
            final int maxDiskSpace = 3000;
            final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
            fileStorageService.startService();
            long fileSize = maxDiskSpace - fileStorageService.getWorkingDataSize() + 1000;
            fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[(int) fileSize]));
        } catch (IOException e) {
            ioException = e;
        }
        assertTrue(ioException != null && ioException.getMessage().equals("Not enough free space in " + STORAGE_PATH));
    }

    @Test
    public void testGetFreeStorageSpace() throws Exception {
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(2000000, STORAGE_PATH);
        fileStorageService.startService();
        final long freeStorageSpace = fileStorageService.getFreeStorageSpace();
        assertTrue(freeStorageSpace > 0 & freeStorageSpace < 2000000);
    }

    @Test
    public void testPurge() throws Exception {

    }

}