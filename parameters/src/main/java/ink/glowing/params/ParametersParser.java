package ink.glowing.params;

import ink.glowing.params.ParameterImpl.ListedImpl;
import ink.glowing.params.ParameterImpl.MappedImpl;
import ink.glowing.params.ParameterImpl.PlainImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Character.isWhitespace;

public final class ParametersParser {
    private static final char NIL = '\0';

    private final char[] input;
    private final int length;
    private int pos;

    private ParametersParser(String inputStr) {
        this.input = inputStr.toCharArray();
        this.length = this.input.length;
    }

    public static Parameter.Mapped parseMap(String inputStr) {
        return new MappedImpl(inputStr, new ParametersParser(inputStr).parseMap(0));
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

    private @NotNull String slice(int start) {
        return new String(input, start, pos - start - 1);
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

    private Map<String, Parameter> parseMap(int start) {
        Map<String, Parameter> map = new LinkedHashMap<>();
        while (hasMore()) {
            char ch = pop();
            if (ch == '\'') {
                String key = parseQuotedString();
                if (advanceOn(':')) {
                    map.put(key, parseSingleValue(start == 0 ? NIL : '}'));
                    continue;
                }
                throw new IllegalArgumentException("Couldn't find semicolon for the map value at pos " + pos);
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
                    map.put(keyBuilder.toString(), parseSingleValue(start == 0 ? NIL : '}'));
                    continue;
                }
            }
            throw new IllegalArgumentException("Couldn't find semicolon for the map value at pos " + pos);
        }
        if (start != 0) {
            throw new IllegalArgumentException("Couldn't find the end of a map started at " + start);
        }
        return map;
    }

    private Parameter parseList() {
        List<Parameter> list = new ArrayList<>();
        int start = pos;
        while (hasMore()) {
            if (!skipWhitespaces()) {
                throw new IllegalArgumentException("Couldn't find the end of a list started at " + start);
            }
            if (advanceOn(']')) {
                return new ListedImpl(slice(start), list);
            }
            list.add(parseSingleValue(']'));
        }
        throw new IllegalArgumentException("Couldn't find the end of a list started at " + start);
    }

    private Parameter parseSingleValue(char parentEnd) {
        if (!skipWhitespaces() || current() == parentEnd) {
            return PlainImpl.EMPTY;
        }
        if (advanceOn('[')) {
            return parseList();
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
        advanceOn('\\');
        stringBuilder.append(pop());

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
