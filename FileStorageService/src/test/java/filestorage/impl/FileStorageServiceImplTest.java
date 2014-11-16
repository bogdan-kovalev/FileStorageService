package filestorage.impl;

import filestorage.StorageException;
import filestorage.impl.exception.FileLockedException;
import filestorage.impl.exception.NotEnoughFreeSpaceException;
import org.junit.Test;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;

import static org.junit.Assert.assertTrue;

public class FileStorageServiceImplTest {

    public static final String STORAGE_PATH = ".".concat(File.separator).concat("storage");

    @Test
    public void testFileLock() throws StorageException, IOException {
        final int maxDiskSpace = 2000000;
        final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        final String file_name = "testFileLock";

        boolean condition = false;

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    fileStorageService.saveFile(file_name, new ByteArrayInputStream(new byte[1000000]));
                } catch (IOException | FileLockedException | NotEnoughFreeSpaceException e) {
                    e.printStackTrace();
                } catch (StorageException e) {
                }

            }
        }).start();

        try {
            fileStorageService.saveFile(file_name, new ByteArrayInputStream(new byte[]{}));
        } catch (FileLockedException e) {
            condition = true;
        }

        assertTrue(condition);
    }

    @Test
    public void testSaveFiles() throws Exception, StorageException {
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
    public void testReadFile() throws IOException, StorageException {
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
    public void testSaveFileWithLiveTime() throws StorageException, InterruptedException, IOException {

        boolean condition = false;

        final int maxDiskSpace = 2000000;
        final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
        fileStorageService.startService();

        final int lifeTimeMillis = 2000;
        final String filename = "fileWithLiveTime";
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{}), lifeTimeMillis);

        Thread.sleep(lifeTimeMillis + 500);

        try {
            fileStorageService.readFile(filename);
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

        try {
            fileStorageService.readFile(filename);
        } catch (FileNotFoundException e) {
            condition = true;
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
        try {
            final int maxDiskSpace = 3000;
            final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(maxDiskSpace, STORAGE_PATH);
            fileStorageService.startService();
            long fileSize = maxDiskSpace - fileStorageService.getWorkingDataSize() + 1000;
            fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[(int) fileSize]));
        } catch (FileLockedException e) {
            e.printStackTrace();
        } catch (NotEnoughFreeSpaceException e) {
            condition = true;
        }
        assertTrue(condition);
    }

    @Test
    public void testGetFreeStorageSpace() throws StorageException {
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(2000000, STORAGE_PATH);
        fileStorageService.startService();
        final long freeStorageSpace = fileStorageService.getFreeStorageSpace();
        assertTrue(freeStorageSpace > 0 & freeStorageSpace < 2000000);
    }

    @Test
    public void testPurge() throws Exception {

    }

}