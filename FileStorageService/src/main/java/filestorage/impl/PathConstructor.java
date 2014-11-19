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

    private static final List<Integer> DIVIDERS = new ArrayList<Integer>() {{
        add(Math.abs(Integer.MIN_VALUE / 64));
        add(get(0) / 128);
        add(get(1) / 128);
    }};

    /**
     * This method returns a path where a file with this 'key' can be stored.
     * Algorithm work basics on the hash-code value of the 'key'. Destination path for the file with the given 'key'
     * represent a nested folders. Level of the nesting equal to the number of the 'DIVIDERS' and it equals 3.
     * The name of each folder in a folders hierarchy echoes to the range of the values of the hash-code that this folder
     * can contains.<p>
     * Example:<p>
     * <i><b>'startFolder'</b>\[-1207959552_-1174405121]\[-1215299584_-1215037441]\[-1215328256_-1215326209]\</i>
     *
     * @param key
     * @param startFolder
     * @return
     */
    public static String calculateDestinationPath(String key, String startFolder) {
        final int hashcode = key.hashCode();

        String path = "";

        for (int div : DIVIDERS) {
            long left_boundary = hashcode - hashcode % div;
            long right_boundary = left_boundary + div - 1;
            path += separator + createName(left_boundary, right_boundary);
        }

        return startFolder.concat(path);
    }

    private static String createName(long left_boundary, long right_boundary) {
        return "[" + left_boundary + "_" + right_boundary + "]";
    }
}
