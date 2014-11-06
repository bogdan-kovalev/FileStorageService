package file_storage.impl;

import java.io.ByteArrayInputStream;
import java.io.File;

public class FileStorageServiceImplTest {

    @org.junit.Test
    public void testSaveFile() throws Exception {
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl(2000000, new File("storage"));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3, 4});
        fileStorageService.saveFile("testSaveFile", inputStream);
    }

    @org.junit.Test
    public void testReadFile() throws Exception {

    }

    @org.junit.Test
    public void testDeleteFile() throws Exception {

    }

    @org.junit.Test
    public void testGetFreeStorageSpace() throws Exception {

    }

    @org.junit.Test
    public void testPurge() throws Exception {

    }
}