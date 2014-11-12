package file_storage.impl;


import file_storage.FileStorageService;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

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

        if (!createStorage()) {
            throw new IOException("Unable to create storage");
        }

        liveTimeWatcher = new LiveTimeWatcher(rootFolder);
        if (liveTimeWatcher.getDataFileSize() > maxDiskSpace)
            throw new IOException("Not enough free space in " + rootFolder);

        new Thread(liveTimeWatcher).start();
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
        writeFile(getFilePath(key), Channels.newChannel(inputStream), inputStream.available());
    }

    /**
     * This method provides possibility to saving data represented by {@code String} 'key' and {@code InputStream} 'inputStream'
     * into disk drive storage.
     * <p/>
     * Algorithm will save 'inputStream' into file in storage by using of the hashcode calculated for String 'key'. File
     * will be deleted after 'liveTimeMillis' milliseconds.
     *
     * @param key            unique id
     * @param inputStream    input stream
     * @param liveTimeMillis live time of the stored file
     * @throws IOException
     */
    @Override
    public void saveFile(String key, InputStream inputStream, long liveTimeMillis) throws IOException {
        final String filePath = getFilePath(key);
        writeFile(filePath, Channels.newChannel(inputStream), inputStream.available());
        liveTimeWatcher.addFile(filePath, liveTimeMillis);
    }

    @Override
    public InputStream readFile(String key) {
        try {
            return Channels.newInputStream(new FileInputStream(getFilePath(key)).getChannel());
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public void deleteFile(String key) {
        new File(getFilePath(key)).delete();
    }

    /**
     * @return free storage space in bytes
     */
    @Override
    public long getFreeStorageSpace() {
        return maxDiskSpace - FileUtils.sizeOfDirectory(new File(rootFolder));
    }

    @Override
    public void purge(double percents) {
        throw new IllegalStateException("Not implemented yet");
    }

    public long getWorkingDataSize() throws IOException {
        return liveTimeWatcher.getDataFileSize();
    }

    private void writeFile(String filePath, ReadableByteChannel channel, long length) throws IOException {
        if (!haveEnoughFreeSpace(length))
            throw new IOException("Not enough free space in " + rootFolder);

        try (final FileChannel out = new FileOutputStream(filePath).getChannel()) {
            final ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (channel.read(buffer) != -1) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
        }
    }

    private boolean haveEnoughFreeSpace(long fileSize) {
        return fileSize < getFreeStorageSpace();
    }

    private String getFilePath(String key) {
        return new PathConstructor().constructFilePathInStorage(key, rootFolder);
    }

    private boolean createStorage() {
        final File root = new File(rootFolder);
        return root.exists() || root.mkdir();
    }
}
