package filestorage.impl;

import com.google.common.base.Ascii;
import com.google.common.escape.ArrayBasedCharEscaper;
import com.google.common.escape.CharEscaper;

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
        CharEscaper replacingEscaper = new ArrayBasedCharEscaper(ILLEGAL_CHARACTERS_REPLACEMENT, Ascii.MIN, Ascii.MAX) {
            @Override
            protected char[] escapeUnsafe(char c) {
                return new char[0];
            }
        };

        return replacingEscaper.escape(input);
    }
}
