package filestorage.impl;

import filestorage.StorageException;
import filestorage.impl.exception.FileLockedException;
import filestorage.impl.exception.InvalidPercentsValueException;
import filestorage.impl.exception.NotEnoughFreeSpaceException;
import org.junit.Test;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;

import static org.junit.Assert.assertTrue;

public class FileStorageServiceImplTest {

    public static final String STORAGE_PATH = ".".concat(File.separator).concat("storage");

    @Test
    public void testSaveFiles() throws StorageException, IOException {
        final int maxDiskSpace = 2000000;
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        String file1_name = "file1_testSaveFiles";
        String file2_name = "file2_testSaveFiles";
        String file3_name = "file3_testSaveFiles";

        fileStorageService.saveFile(file1_name, new ByteArrayInputStream(new byte[]{}));
        fileStorageService.saveFile(file2_name, new ByteArrayInputStream(new byte[]{}));
        fileStorageService.saveFile(file3_name, new ByteArrayInputStream(new byte[]{}));

        try (final InputStream inputStream = fileStorageService.readFile(file1_name);
             final InputStream inputStream1 = fileStorageService.readFile(file2_name);
             final InputStream inputStream2 = fileStorageService.readFile(file3_name)) {

            assertTrue(inputStream != null);
            assertTrue(inputStream1 != null);
            assertTrue(inputStream2 != null);
        }

        fileStorageService.deleteFile(file1_name);
        fileStorageService.deleteFile(file2_name);
        fileStorageService.deleteFile(file3_name);
    }

    @Test
    public void testReadFile() throws IOException, StorageException {
        final int maxDiskSpace = 2000000;
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        String filename = "testReadFile";
        final byte testByte = 77;
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{testByte}));

        try (final InputStream file = fileStorageService.readFile(filename)) {
            assertTrue(file.read() == testByte);
        }

        fileStorageService.deleteFile(filename);
    }

    @Test
    public void testSaveFileWithLiveTime() throws StorageException, InterruptedException, IOException {

        boolean condition = false;

        final int maxDiskSpace = 2000000;
        final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        final int lifeTimeMillis = 2000;
        final String filename = "fileWithLiveTime";
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{}), lifeTimeMillis);

        Thread.sleep(lifeTimeMillis + 500);

        try (final InputStream inputStream = fileStorageService.readFile(filename)) {
        } catch (FileNotFoundException e) {
            condition = true;
        }

        assertTrue(condition);
    }

    @Test
    public void testDeleteFile() throws StorageException, FileAlreadyExistsException {

        boolean condition = false;

        final int maxDiskSpace = 2000000;
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        String filename = "testDeleteFile";
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{}));

        fileStorageService.deleteFile(filename);

        try (final InputStream inputStream = fileStorageService.readFile(filename)) {
        } catch (FileNotFoundException e) {
            condition = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(condition);
    }

    @Test
    public void testStorageNotEnoughSpaceToCreateStorage() throws StorageException {
        boolean condition = false;
        try {
            final int maxDiskSpace = 5;
            final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
            fileStorageService.startService();
        } catch (NotEnoughFreeSpaceException e) {
            condition = true;
        }
        assertTrue(condition);
    }

    @Test
    public void testStorageNotEnoughSpaceForFileSave() throws StorageException, FileAlreadyExistsException {
        final String filename = "testStorageSize";
        boolean condition = false;
        final int maxDiskSpace = 3000;
        final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        try {
            fileStorageService.startService();
            long fileSize = maxDiskSpace - fileStorageService.getSystemDataSize() + 1000;
            fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[(int) fileSize]));
        } catch (FileLockedException e) {
            e.printStackTrace();
        } catch (NotEnoughFreeSpaceException e) {
            condition = true;
        }
        assertTrue(condition);
        fileStorageService.deleteFile(filename);
    }

    @Test
    public void testGetFreeStorageSpace() throws StorageException {
        final int maxDiskSpace = 2000000;
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();
        final long freeStorageSpace = fileStorageService.getFreeStorageSpace();
        assertTrue(freeStorageSpace > 0 & freeStorageSpace < maxDiskSpace);
    }

    @Test
    public void testPurge() throws StorageException, FileAlreadyExistsException, InterruptedException, InvalidPercentsValueException {
        final int maxDiskSpace = 25000;
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();
        final String filename = "purgeFile";
        int i = 0;

        while (fileStorageService.getFreeStorageSpace() > maxDiskSpace * 0.1) {
            final String key = filename + i++;
            fileStorageService.saveFile(key, new ByteArrayInputStream(new byte[100]));
        }

        final float percents = 0.3f;
        fileStorageService.purge(percents);

        final long freeSpaceNow = fileStorageService.getFreeStorageSpace();
        final long systemDataSize = fileStorageService.getSystemDataSize();

        assertTrue(freeSpaceNow >= maxDiskSpace * percents - systemDataSize);
    }

}