package ink.glowing.params;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Character.isWhitespace;

public class ParameterParser {
    final String inputStr;
    final char[] input;
    final int length;
    int pos;

    private ParameterParser(String inputStr) {
        this.inputStr = inputStr;
        this.input = inputStr.toCharArray();
        this.length = this.input.length;
    }

    public static Parameter<?> parse(String raw) {
        return new Parameter.OfMap(new ParameterParser(raw).parseMap(true), true);
    }

    private void advance() {
        ++pos;
    }

    private boolean advanceOn(char ch) {
        if (currentSafe() == ch) {
            this.pos++;
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
        return eof() ? '\0' : input[pos];
    }

    private boolean eof() {
        return pos >= length;
    }

    private void back() {
        --pos;
    }

    private boolean skipWhitespaces() {
        while (!eof()) {
            if (isWhitespace(current())) {
                advance();
            } else {
                return true;
            }
        }
        return false;
    }

    private Map<String, Parameter<?>> parseMap(boolean global) {
        Map<String, Parameter<?>> map = new LinkedHashMap<>();
        while (!eof()) {
            char ch = pop();
            if (ch == '\'') {
                String key = parseQuotedString();
                if (advanceOn(':')) {
                    map.put(key, parseSingleValue(global ? '\0' : '}'));
                } else {
                    throw new IllegalArgumentException("Couldn't find semicolon for the map value at pos " + pos);
                }
                continue;
            } else if (ch == '}') {
                if (global) {
                    throw new IllegalArgumentException("Found trailing '}' while parsing global map at pos " + (pos - 1));
                }
                return map;
            } else if (isWhitespace(ch)) {
                continue;
            }

            StringBuilder keyBuilder = new StringBuilder();
            if (ch == '\\') {
                if (eof()) {
                    throw new IllegalArgumentException("Found escaping '\\' at the end of a string");
                }
                keyBuilder.append(pop());
            } else {
                keyBuilder.append(ch);
            }
            while (!eof()) {
                char chKey = pop();
                if (chKey == ':' || isWhitespace(chKey)) {
                    back();
                    break;
                }
                if (chKey == '\\') {
                    if (eof()) {
                        throw new IllegalArgumentException("Found escaping '\\' at the end of a string");
                    }
                    keyBuilder.append(pop());
                    continue;
                }
                keyBuilder.append(chKey);
            }
            if (skipWhitespaces()) {
                if (advanceOn(':')) {
                    map.put(keyBuilder.toString(), parseSingleValue(global ? '\0' : '}'));
                    continue;
                }
            }
            throw new IllegalArgumentException("Couldn't find semicolon for the map value at pos " + pos);
        }
        return map;
    }

    private Parameter<?> parseList() {
        List<Parameter<?>> list = new ArrayList<>();
        while (!eof()) {
            if (advanceOn(']')) break;
            list.add(parseSingleValue(']'));
        }
        return new Parameter.OfList(list);
    }

    private Parameter<?> parseSingleValue(char parentEnd) {
        if (!skipWhitespaces() || current() == parentEnd) {
            return Parameter.Empty.INSTANCE;
        }
        char startCh = current();
        if (advanceOn('[')) {
            return parseList();
        } else if (advanceOn('{')) {
            return new Parameter.OfMap(parseMap(false));
        } else if (advanceOn('\'')) {
            String string = parseQuotedString();
            if (skipWhitespaces() && advanceOn(':')) { // Singleton map
                return new Parameter.OfMap(Map.of(string, parseSingleValue(parentEnd)));
            }
            return new Parameter.OfString(string);
        }

        StringBuilder stringBuilder = new StringBuilder();
        if (advanceOn('\\')) {
            stringBuilder.append(current());
        } else {
            stringBuilder.append(startCh);
        }
        advance();
        
        while (!eof()) {
            char ch = pop();
            if (ch == ':') { // Singleton map
                return new Parameter.OfMap(Map.of(stringBuilder.toString(), parseSingleValue('\0')));
            } else if (isWhitespace(ch)) {
                break;
            } else if (ch == '\\') {
                if (eof()) {
                    throw new IllegalArgumentException("Found escaping '\\' at the end of a string");
                }
                stringBuilder.append(pop());
                continue;
            } else if (ch == parentEnd) {
                back(); // Allow parent to handle the closing
                break;
            }
            stringBuilder.append(ch);
        }
        return new Parameter.OfString(stringBuilder.toString());
    }
    
    private String parseQuotedString() {
        StringBuilder stringBuilder = new StringBuilder();
        while (!eof()) {
            char ch = pop();
            if (ch == '\'') {
                return stringBuilder.toString();
            } if (ch == '\\') {
                stringBuilder.append(pop());
                continue;
            }
            stringBuilder.append(ch);
        }
        throw new IllegalArgumentException("Couldn't find the end of a quoted string");
    }
}
