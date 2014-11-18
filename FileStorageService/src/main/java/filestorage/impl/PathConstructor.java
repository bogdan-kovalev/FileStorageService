package filestorage.impl;

import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;

/**
 * Maximum 2047 files in the lowest level folder.
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
     * This method returns the path where the file this key will be saved.
     * The depth of folders hierarchy equals to the number of the DIVIDERS.
     * Each folder can contain a file with the hashcode that enters the subband hashcode value for this folder.
     * Subband hash code value for the folder at this level of nesting is calculated by dividing the range of values
     * of the hashcode of the key on the appropriate divider.
     *
     * @param key
     * @param startFolder
     * @return
     */
    public static String findDestinationPath(String key, String startFolder) {
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
