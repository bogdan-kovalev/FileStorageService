package file_storage.impl;


import file_storage.FileStorageService;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

import static java.io.File.separator;

/**
 * @author Bogdan Kovalev
 */
public class FileStorageServiceImpl implements FileStorageService {

    private final long maxDiskSpace;
    private final String rootFolder;

    private LiveTimeWatcher liveTimeWatcher;

    /**
     * @param maxDiskSpace max disk space can be used (in bytes)
     * @param rootFolder   root folder of file system
     */
    public FileStorageServiceImpl(long maxDiskSpace, String rootFolder) throws IOException {
        this.maxDiskSpace = maxDiskSpace;
        this.rootFolder = rootFolder;
        final File rootDir = new File(rootFolder);
        if (!rootDir.exists() && !rootDir.mkdir()) {
            throw new IOException("Unable to create storage");
        }

        liveTimeWatcher = new LiveTimeWatcher(rootFolder);
        final Thread thread = new Thread(liveTimeWatcher);
        thread.start();
    }

    /**
     * This method provides possibility to saving data represented by {@code String} 'key' and {@code InputStream} 'inputStream'
     * into disk drive storage.
     * <p/>
     * Algorithm will save 'inputStream' into file in storage by using of the hashcode calculated for String 'key'.
     *
     * @param key         unique id
     * @param inputStream input stream
     * @throws IOException
     */
    @Override
    public void saveFile(String key, InputStream inputStream) throws IOException {

        if (!haveEnoughFreeSpace(inputStream.available()))
            throw new IOException("Not enough free space in " + rootFolder);

        String destination = new PathConstructor().constructPathInStorage(key.hashCode());

        new File(rootFolder.concat(separator).concat(destination)).mkdirs();

        final String filePath = rootFolder.concat(separator).concat(destination).concat(separator).concat(key);

        writeFile(filePath, Channels.newChannel(inputStream));
    }

    @Override
    public void saveFile(String key, InputStream inputStream, long liveTimeMillis) throws IOException {
        saveFile(key, inputStream);
        liveTimeWatcher.addFile(getFilePath(key), liveTimeMillis);
    }

    private void writeFile(String filePath, ReadableByteChannel channel) throws IOException {
        try (final FileChannel out = new FileOutputStream(filePath).getChannel()) {
            final ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (channel.read(buffer) != -1) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
        }
    }

    private boolean haveEnoughFreeSpace(int fileSize) {
        return FileUtils.sizeOfDirectory(new File(rootFolder)) + fileSize < maxDiskSpace;
    }

    @Override
    public InputStream readFile(String key) throws FileNotFoundException {
        String sourceFolderPath = new PathConstructor().constructPathInStorage(key.hashCode());
        String filePath = rootFolder.concat(separator).concat(sourceFolderPath).concat(separator).concat(key);

        final FileChannel in = new FileInputStream(filePath).getChannel();
        return Channels.newInputStream(in);
    }

    @Override
    public void deleteFile(String key) throws IOException {
        String sourceFolderPath = new PathConstructor().constructPathInStorage(key.hashCode());
        String filePath = rootFolder.concat(separator).concat(sourceFolderPath).concat(separator).concat(key);
        final File file = new File(filePath);
        Files.deleteIfExists(file.toPath());
    }

    /**
     * @return free storage space in bytes
     */
    @Override
    public long getFreeStorageSpace() {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void purge(double percents) {
        throw new IllegalStateException("Not implemented yet");
    }

    public String getFilePath(String key) {
        String sourceFolder = new PathConstructor().constructPathInStorage(key.hashCode());
        String filePath = rootFolder.concat(separator).concat(sourceFolder).concat(separator).concat(key);
        return filePath;
    }

}
