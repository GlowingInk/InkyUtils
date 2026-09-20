package ink.glowing.utils.params;

import ink.glowing.utils.CaseInsensitive;
import ink.glowing.utils.params.ParameterImpl.ListedImpl;
import ink.glowing.utils.params.ParameterImpl.MappedImpl;
import ink.glowing.utils.params.ParameterImpl.PlainImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Character.isWhitespace;

final class ParserImpl {
    private static final char NIL = '\0';

    private final char[] input;
    private final int length;
    private int pos;

    ParserImpl(char[] input) {
        this.input = input;
        this.length = input.length;
    }

    private void advance() {
        ++pos;
    }

    private void back() {
        --pos;
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

    private @NotNull Function<Parameter, String> slice(int start) {
        return new ParameterImpl.LazyValue(input, start, pos);
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

    private boolean handleEscaping(char ch, StringBuilder stringBuilder) {
        if (ch == '\\') {
            if (!hasMore()) {
                throw new IllegalArgumentException("Found escaping '\\' at the end of a string");
            }
            stringBuilder.append(pop());
            return true;
        }
        return false;
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

            StringBuilder keyBuilder = new StringBuilder();
            if (!handleEscaping(ch, keyBuilder)) {
                keyBuilder.append(ch);
            }
            while (hasMore()) {
                char chKey = pop();
                if (chKey == ':' || isWhitespace(chKey)) {
                    back();
                    break;
                }
                if (handleEscaping(chKey, keyBuilder)) {
                    continue;
                }
                keyBuilder.append(chKey);
            }
            if (skipWhitespaces()) {
                if (advanceOn(':')) {
                    map.put(keyBuilder.toString(), parseSingleValue(endCh));
                    continue;
                }
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
        if (!skipWhitespaces() || current() == parentEnd) {
            return PlainImpl.EMPTY;
        }
        if (advanceOn('[')) {
            int start = pos;
            var value = parseList(start);
            return new ListedImpl(slice(start), value);
        } else if (advanceOn('{')) {
            int start = pos;
            var value = parseMap(start);
            return new MappedImpl(slice(start), value);
        } else if (advanceOn('\'')) {
            String string = parseQuotedString();
            if (skipWhitespaces() && advanceOn(':')) { // Singleton map
                int start = pos;
                var value = Map.of(string, parseSingleValue(parentEnd));
                return new MappedImpl(slice(start), value);
            }
            return new PlainImpl(string);
        }

        StringBuilder stringBuilder = new StringBuilder();
        char first = pop();
        if (!handleEscaping(first, stringBuilder)) {
            stringBuilder.append(first);
        }

        while (hasMore()) {
            char ch = pop();
            if (ch == ':') { // Singleton map
                int start = pos;
                var value = Map.of(stringBuilder.toString(), parseSingleValue(NIL));
                return new MappedImpl(slice(start), value);
            } else if (isWhitespace(ch)) {
                break;
            } else if (handleEscaping(ch, stringBuilder)) {
                continue;
            } else if (ch == parentEnd) {
                back(); // Allow parent to handle the closing
                break;
            }
            stringBuilder.append(ch);
        }
        return new PlainImpl(stringBuilder.toString());
    }

    private String parseQuotedString() {
        StringBuilder stringBuilder = new StringBuilder();
        int start = pos;
        while (hasMore()) {
            char ch = pop();
            if (ch == '\'') {
                return stringBuilder.toString();
            } if (handleEscaping(ch, stringBuilder)) {
                continue;
            }
            stringBuilder.append(ch);
        }
        throw new IllegalArgumentException("Couldn't find the end of a quoted string started at " + start);
    }
}