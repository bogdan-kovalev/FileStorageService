package file_storage.impl;

import java.io.File;

import static java.io.File.separator;

/**
 * @author Bogdan Kovalev.
 */
public class PathConstructor {

    private static final int FIRST_HASHCODE_WINDOW_LENGTH = Math.abs(Integer.MIN_VALUE / 32);
    private static final int SECOND_HASHCODE_WINDOW_LENGTH = FIRST_HASHCODE_WINDOW_LENGTH / 32;
    private static final int THIRD_HASHCODE_WINDOW_LENGTH = SECOND_HASHCODE_WINDOW_LENGTH / 64;

    private long left_boundary;
    private long right_boundary;


    public String constructFilePathInStorage(String key, String rootFolder) {
        final int hashcode = key.hashCode();

        left_boundary = Integer.MIN_VALUE;
        right_boundary = Integer.MAX_VALUE;

        String destination = find(FIRST_HASHCODE_WINDOW_LENGTH, hashcode);
        if (destination == "") throw new IllegalStateException("Hashcode out of range");
        destination += separator.concat(find(SECOND_HASHCODE_WINDOW_LENGTH, hashcode));
        destination += separator.concat(find(THIRD_HASHCODE_WINDOW_LENGTH, hashcode));

        final String destinationPathInStorage = rootFolder.concat(separator).concat(destination);

        new File(destinationPathInStorage).mkdirs();

        final String filePath = destinationPathInStorage.concat(separator).concat(key);

        return filePath;
    }

    private String find(int range, int hashcode) {
        long right_temp_boundary = left_boundary + range;

        while (right_temp_boundary < right_boundary + 2) {
            if (hashcode >= left_boundary & hashcode < right_temp_boundary) {
                right_boundary = right_temp_boundary;
                return createName(left_boundary, right_temp_boundary);
            }
            left_boundary = right_temp_boundary;
            right_temp_boundary += range;
        }
        return "";
    }

    private String createName(long left_boundary, long right_boundary) {
        return "[" + left_boundary + "_" + right_boundary + "]";
    }
}
