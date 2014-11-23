package filestorage.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bogdan Kovalev
 */
public class FileNameValidator {

    private static final Map<Character, String> ILLEGAL_CHARACTERS_REPLACEMENT = new HashMap<Character, String>() {{
        put('/', "_a");
        put('\n', "_b");
        put('\r', "_c");
        put('\t', "_d");
        put('\0', "_e");
        put('\f', "_f");
        put('`', "_g");
        put('?', "_h");
        put('*', "_i");
        put('\\', "_j");
        put('<', "_k");
        put('>', "_l");
        put('|', "_m");
        put('\"', "_n");
        put(':', "_o");
    }};

    public static String validate(String input) {
        if (input == null) return null;

        StringBuilder out = new StringBuilder();

        for (char c : input.toCharArray()) {
            final String replacement = ILLEGAL_CHARACTERS_REPLACEMENT.get(c);
            if (replacement == null) {
                out.append(c);
            } else {
                out.append(replacement);
            }
        }

        return out.toString();
    }
}
