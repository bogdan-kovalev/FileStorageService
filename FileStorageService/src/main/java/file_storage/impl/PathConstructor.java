package file_storage.impl;

import static java.io.File.separator;

/**
 * @author Bogdan Kovalev.
 */
public class PathConstructor {

    private static final int FIRST_RANGE = Math.abs(Integer.MIN_VALUE / 32);
    private static final int SECOND_RANGE = FIRST_RANGE / 32;
    private static final int THIRD_RANGE = SECOND_RANGE / 64;

    private long left_range;
    private long right_range;

    public String constructPathInStorage(int hashcode) {
        String path = "";

        left_range = Integer.MIN_VALUE;
        right_range = Integer.MAX_VALUE;

        path += findPath(FIRST_RANGE, hashcode);
        if (path == "")
            throw new IllegalStateException("Hashcode is out of range " + Integer.MIN_VALUE + "..." + Integer.MAX_VALUE);

        path += separator.concat(findPath(SECOND_RANGE, hashcode));
        path += separator.concat(findPath(THIRD_RANGE, hashcode));

        return path;
    }

    private String findPath(int range_unit, int hashcode) {
        String out = "";

        long right_temp_range = left_range + range_unit;

        while (right_temp_range < right_range + 2) {
            if (hashcode >= left_range & hashcode < right_temp_range) {
                out = "[" + left_range + "_" + right_temp_range + "]";
                break;
            }
            left_range = right_temp_range;
            right_temp_range += range_unit;
        }
        right_range = right_temp_range;
        return out;
    }
}
