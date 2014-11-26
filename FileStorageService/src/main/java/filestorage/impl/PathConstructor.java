package filestorage.impl;

import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;

/**
 * This is util class. It calculates a destination path for storing the file with the given 'key'.
 *
 * @author Bogdan Kovalev.
 */
public class PathConstructor {

    private static final long NUMBER_OF_HASHCODES = Math.abs((long) Integer.MIN_VALUE) + Integer.MAX_VALUE + 1;
    private static final int DEFAULT_DEPTH = 3;
    private static final int DEFAULT_LEVEL_CAPACITY = 128;

    private final List<Integer> DIVIDERS = new ArrayList<>();

    /**
     * @param depth         - level of the folders nesting
     * @param levelCapacity - maximum number of subdirectories in directory on each level of nesting
     */
    public PathConstructor(int depth, int levelCapacity) {
        if (depth == 0) throw new IllegalStateException("PathConstructor: Invalid depth value");

        DIVIDERS.add(0, (int) (NUMBER_OF_HASHCODES / levelCapacity));
        for (int i = 1; i < depth; i++) {
            DIVIDERS.add(i, DIVIDERS.get(i - 1) / levelCapacity);
        }
    }

    public PathConstructor() {
        this(DEFAULT_DEPTH, DEFAULT_LEVEL_CAPACITY);
    }

    /**
     * This method returns a path where a file with this 'key' can be stored.
     * Algorithm work basics on the unsigned hash-code value of the 'key'. Destination path for the file with the given 'key'
     * represent a nested folders. Level of the nesting equals to the number of the 'DIVIDERS' and it equals 3 (DEFAULT_DEPTH).
     * The name of each folder in a folders hierarchy echoes to the range of the values of the hash-code that this folder
     * can contains.<p>
     * Example:<p>
     * <i><b>'startFolder'</b>\[1207959552_1174405121]\[1215299584_1215037441]\[1215328256_1215326209]\</i>
     *
     * @param key
     * @param startFolder
     * @return
     */
    public String calculateDestinationPath(String key, String startFolder) {
        final long unsigned_hashcode = key.hashCode() + Math.abs((long) Integer.MIN_VALUE);

        String path = "";

        for (int div : DIVIDERS) {
            long left_boundary = unsigned_hashcode - unsigned_hashcode % div;
            long right_boundary = left_boundary + div - 1;
            path += separator + createName(left_boundary, right_boundary);
        }

        return startFolder.concat(path);
    }

    private static String createName(long left_boundary, long right_boundary) {
        return "[" + left_boundary + "_" + right_boundary + "]";
    }
}
