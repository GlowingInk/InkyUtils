package ink.glowing.utils.params;

import ink.glowing.utils.hash.CaseInsensitive;
import ink.glowing.utils.params.ParameterImpl.ListedImpl;
import ink.glowing.utils.params.ParameterImpl.MappedImpl;
import ink.glowing.utils.params.ParameterImpl.ValueImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Character.isWhitespace;

final class ParserImpl {
    static final int MAX_DEPTH = Math.max(1, Integer.getInteger("ink.glowing.utils.params.maxDepth", 512));

    private static final char NIL = '\0';

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
        if (currentSafe() == ch) {
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

    private char currentSafe() {
        return hasMore() ? input[pos] : NIL;
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
        if (!escaped) return new String(input, start, end - start);

        StringBuilder stringBuilder = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char ch = input[i];
            if (ch == '\\') ch = input[++i]; // guaranteed safe by skipEscape
            stringBuilder.append(ch);
        }
        return stringBuilder.toString();
    }

    private int scanBare(int start, char stopAt) {
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

    private static @NotNull Map<String, Parameter> singletonMap(@NotNull String key, @NotNull Parameter value) {
        Map<String, Parameter> map = CaseInsensitive.newLinkedMap(1);
        map.put(key, value);
        return map;
    }

    @NotNull Map<String, Parameter> parseMap(final int start) {
        Map<String, Parameter> map = CaseInsensitive.newLinkedMap();
        char endCh = start == 0 ? NIL : '}';
        while (hasMore()) {
            char ch = pop();
            if (ch == '\'') {
                String key = parseQuotedString();
                if (advanceOn(':')) {
                    map.put(key, parseSingleValue(endCh));
                    continue;
                }
                throw new IllegalArgumentException("Couldn't find colon for the map value at pos " + pos);
            } else if (ch == '}') {
                if (start == 0) {
                    throw new IllegalArgumentException("Found trailing '}' while parsing global map at pos " + (pos - 1));
                }
                return map;
            } else if (isWhitespace(ch)) {
                continue;
            }

            int keyStart = pos - 1;
            int keyEnd = scanBare(keyStart, ':');
            String key = unescape(keyStart, keyEnd, scanEscaped);
            pos = keyEnd;
            if (advanceOn(':')) {
                map.put(key, parseSingleValue(endCh));
                continue;
            }
            throw new IllegalArgumentException("Couldn't find colon for the map value at pos " + pos);
        }
        if (start != 0) {
            throw new IllegalArgumentException("Couldn't find the end of a map started at " + start);
        }
        return map;
    }

    @NotNull List<Parameter> parseList(final int start) {
        List<Parameter> list = new ArrayList<>();
        char endCh = start == 0 ? NIL : ']';
        while (hasMore()) {
            if (!skipWhitespaces()) {
                break;
            }
            if (advanceOn(']')) {
                if (start == 0) {
                    throw new IllegalArgumentException("Found trailing ']' while parsing global list at pos " + (pos - 1));
                }
                return list;
            }
            list.add(parseSingleValue(endCh));
        }
        if (start != 0) {
            throw new IllegalArgumentException("Couldn't find the end of a list started at " + start);
        }
        return list;
    }

    private Parameter parseSingleValue(char parentEnd) {
        if (++depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Nesting is deeper than " + MAX_DEPTH + " levels at pos " + pos);
        }
        try {
            return parseSingleValueUnchecked(parentEnd);
        } finally {
            --depth;
        }
    }

    private Parameter parseSingleValueUnchecked(char parentEnd) {
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
                var value = singletonMap(string, parseSingleValue(parentEnd));
                return new MappedImpl(slice(tokenStart, valueEnd), value);
            }
            valueEnd = quotedEnd;
            return new ValueImpl(string, slice(tokenStart, quotedEnd));
        }

        int end = scanBare(tokenStart, parentEnd);
        boolean escaped = scanEscaped;
        String string = unescape(tokenStart, end, escaped);
        if (end < length && input[end] == ':') { // Singleton map
            pos = end + 1;
            var value = singletonMap(string, parseSingleValue(NIL));
            return new MappedImpl(slice(tokenStart, valueEnd), value);
        }

        valueEnd = end;
        pos = end < length && isWhitespace(input[end]) ? end + 1 : end; // the parent handles its closing
        // Without quotes and escapes, the raw string is the value itself
        return new ValueImpl(string, escaped ? slice(tokenStart, end) : ParameterImpl.VALUE_AS_RAW);
    }

    private String parseQuotedString() {
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
