package ink.glowing.utils.params;

import ink.glowing.utils.TextUtils;
import ink.glowing.utils.hash.CaseInsensitive;
import ink.glowing.utils.params.ParameterImpl.ListedImpl;
import ink.glowing.utils.params.ParameterImpl.MappedImpl;
import ink.glowing.utils.params.ParameterImpl.ValueImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.lang.Character.isWhitespace;

final class ParserImpl {
    static final int MAX_DEPTH = Math.max(1, Integer.getInteger("ink.glowing.utils.params.maxDepth", 512));

    private static final int NONE = -1;

    private final char[] input;
    private final int length;
    private int pos;
    private int depth;
    private int valueEnd; // where the value last parsed by parseSingleValue ended, exclusive
    private boolean scanEscaped; // whether the range last scanned by scanBare has escapes

    ParserImpl(char[] input) {
        this.input = input;
        this.length = input.length;
    }

    private void advance() {
        ++pos;
    }

    private boolean advanceOn(char ch) {
        if (hasMore() && input[pos] == ch) {
            ++pos;
            return true;
        }
        return false;
    }

    private char current() {
        return input[pos];
    }

    private char pop() {
        char ch = input[pos];
        ++pos;
        return ch;
    }

    private boolean hasMore() {
        return pos < length;
    }

    private @NotNull Function<Parameter, String> slice(int start, int end) {
        return new ParameterImpl.LazyValue(input, start, end);
    }

    private boolean skipWhitespaces() {
        while (hasMore()) {
            if (isWhitespace(current())) {
                advance();
            } else {
                return true;
            }
        }
        return false;
    }

    private int skipEscape(int index) {
        if (index + 1 >= length) {
            throw new IllegalArgumentException("Found escaping '\\' at the end of a string");
        }
        return index + 2;
    }

    private @NotNull String unescape(int start, int end, boolean escaped) {
        if (!escaped) return TextUtils.substring(input, start, end);

        StringBuilder stringBuilder = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char ch = input[i];
            if (ch == '\\') ch = input[++i]; // guaranteed safe by skipEscape
            stringBuilder.append(ch);
        }
        return stringBuilder.toString();
    }

    private int scanBare(int start, int stopAt) {
        boolean escaped = false;
        int i = start;
        if (input[i] == '\\') {
            escaped = true;
            i = skipEscape(i);
        } else {
            i++;
        }

        while (i < length) {
            char ch = input[i];
            if (ch == '\\') {
                escaped = true;
                i = skipEscape(i);
            } else if (ch == ':' || ch == stopAt || isWhitespace(ch)) {
                break;
            } else {
                i++;
            }
        }
        scanEscaped = escaped;
        return i;
    }

    private @NotNull Parameter parseSingleton(@NotNull String key, int tokenStart, int parentEnd) {
        Object2ObjectLinkedOpenCustomHashMap<String, Parameter> map = CaseInsensitive.newLinkedMap(1);
        map.put(key, parseSingleValue(parentEnd));
        return new MappedImpl(slice(tokenStart, valueEnd), map);
    }

    @NotNull Object2ObjectLinkedOpenCustomHashMap<String, Parameter> parseMap(final int start) {
        Object2ObjectLinkedOpenCustomHashMap<String, Parameter> map = CaseInsensitive.newLinkedMap();
        boolean global = start == 0;
        int endCh = global ? NONE : '}';
        while (hasMore()) {
            char ch = pop();
            if (ch == '}') {
                if (global) {
                    throw new IllegalArgumentException("Found trailing '}' while parsing global map at pos " + (pos - 1));
                }
                return map;
            } else if (isWhitespace(ch)) {
                continue;
            }

            String key;
            if (ch == '\'') {
                key = parseQuotedString();
            } else {
                int keyStart = pos - 1;
                int keyEnd = scanBare(keyStart, ':');
                key = unescape(keyStart, keyEnd, scanEscaped);
                pos = keyEnd;
            }
            if (!advanceOn(':')) {
                throw new IllegalArgumentException("Couldn't find colon for the map value at pos " + pos);
            }
            map.put(key, parseSingleValue(endCh));
        }
        if (!global) {
            throw new IllegalArgumentException("Couldn't find the end of a map started at " + start);
        }
        return map;
    }

    @NotNull List<Parameter> parseList(final int start) {
        List<Parameter> list = new ArrayList<>();
        boolean global = start == 0;
        int endCh = global ? NONE : ']';
        while (hasMore()) {
            if (!skipWhitespaces()) {
                break;
            }
            if (advanceOn(']')) {
                if (global) {
                    throw new IllegalArgumentException("Found trailing ']' while parsing global list at pos " + (pos - 1));
                }
                return list;
            }
            list.add(parseSingleValue(endCh));
        }
        if (!global) {
            throw new IllegalArgumentException("Couldn't find the end of a list started at " + start);
        }
        return list;
    }

    private @NotNull Parameter parseSingleValue(int parentEnd) {
        if (++depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Nesting is deeper than " + MAX_DEPTH + " levels at pos " + pos);
        }
        try {
            return parseSingleValueUnchecked(parentEnd);
        } finally {
            --depth;
        }
    }

    private @NotNull Parameter parseSingleValueUnchecked(int parentEnd) {
        int entry = pos;
        if (!skipWhitespaces() || current() == parentEnd) {
            valueEnd = entry;
            return ValueImpl.EMPTY;
        }

        int tokenStart = pos;
        if (advanceOn('[')) {
            var value = parseList(pos);
            valueEnd = pos;
            return new ListedImpl(slice(tokenStart, valueEnd), value);
        } else if (advanceOn('{')) {
            var value = parseMap(pos);
            valueEnd = pos;
            return new MappedImpl(slice(tokenStart, valueEnd), value);
        } else if (advanceOn('\'')) {
            String string = parseQuotedString();
            int quotedEnd = pos;
            if (advanceOn(':')) { // Singleton map
                return parseSingleton(string, tokenStart, parentEnd);
            }
            valueEnd = quotedEnd;
            return new ValueImpl(string, slice(tokenStart, quotedEnd));
        }

        int end = scanBare(tokenStart, parentEnd);
        boolean escaped = scanEscaped;
        String string = unescape(tokenStart, end, escaped);
        if (end < length && input[end] == ':') { // Singleton map
            pos = end + 1;
            return parseSingleton(string, tokenStart, NONE);
        }

        valueEnd = end;
        pos = end < length && isWhitespace(input[end]) ? end + 1 : end; // the parent handles its closing
        // Without quotes and escapes, the raw string is the value itself
        return new ValueImpl(string, escaped ? slice(tokenStart, end) : ParameterImpl.VALUE_RAW);
    }

    private @NotNull String parseQuotedString() {
        int start = pos;
        boolean escaped = false;
        int i = start;
        while (i < length) {
            char ch = input[i];
            if (ch == '\'') {
                pos = i + 1;
                return unescape(start, i, escaped);
            } else if (ch == '\\') {
                escaped = true;
                i = skipEscape(i);
            } else {
                i++;
            }
        }
        throw new IllegalArgumentException("Couldn't find the end of a quoted string started at " + start);
    }
}
