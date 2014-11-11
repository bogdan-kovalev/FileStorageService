package file_storage.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileStorageServiceImplTest {

    public static final String STORAGE_PATH = ".".concat(File.separator).concat("storage");

    @org.junit.Test
    public void testSaveFiles() throws Exception {
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(2000000, STORAGE_PATH);

        String file1_name = "file1_test";
        String file2_name = "file2_test";
        String file3_name = "file3_test";

        fileStorageService.saveFile(file1_name, new ByteArrayInputStream(new byte[]{}));
        fileStorageService.saveFile(file2_name, new ByteArrayInputStream(new byte[]{}));
        fileStorageService.saveFile(file3_name, new ByteArrayInputStream(new byte[]{}));

        assertTrue(new File(fileStorageService.getFilePath(file1_name)).exists());
        assertTrue(new File(fileStorageService.getFilePath(file2_name)).exists());
        assertTrue(new File(fileStorageService.getFilePath(file3_name)).exists());

    }

    @org.junit.Test
    public void testSaveFileWithLiveTime() throws IOException, InterruptedException {
        final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(2000000, STORAGE_PATH);

        final int liveTimeMillis = 2000;
        final String filename = "fileWithLiveTime";
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{}), liveTimeMillis);

        Thread.sleep(liveTimeMillis + 100);

        assertFalse(new File(fileStorageService.getFilePath(filename)).exists());
    }

    @org.junit.Test
    public void testReadFile() throws IOException {
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(2000000, STORAGE_PATH);

        String filename = "testReadFile";
        final byte testByte = 77;
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{testByte}));

        final InputStream file = fileStorageService.readFile(filename);

        assertTrue(file.read() == testByte);

    }

    @org.junit.Test
    public void testDeleteFile() throws IOException {
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(2000000, STORAGE_PATH);

        String filename = "testDeleteFile";
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{}));
        fileStorageService.deleteFile(filename);

        assertFalse(new File(fileStorageService.getFilePath(filename)).exists());

    }

    @org.junit.Test
    public void testGetFreeStorageSpace() throws Exception {

    }

    @org.junit.Test
    public void testPurge() throws Exception {

    }

}