package ink.glowing.utils.params;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static ink.glowing.utils.params.Parameter.Mapped.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParameterTest {
    public static Stream<Arguments> parseData() {
        return Stream.of(
                Arguments.of(
                        "simple:value",
                        "simple:value"
                ),
                Arguments.of(
                        "first:value second:value",
                        "first:value second:value"
                ),
                Arguments.of(
                        "escaping:\\'\\ space!",
                        "escaping:'\\' space!'"
                ),
                Arguments.of(
                        "list:['of' 'values']",
                        "list:[of values]"
                ),
                Arguments.of(
                        "brackets:'a{b}[c]'",
                        "brackets:'a{b}[c]'"
                ),
                Arguments.of(
                        "mixed:'it\\'s [x]'",
                        "mixed:'it\\'s [x]'"
                ),
                Arguments.of(
                        "tab:'a\tb'",
                        "tab:'a\tb'"
                ),
                Arguments.of(
                        "empty:''",
                        "empty:''"
                ),
                Arguments.of(
                        "colon:'a:b'",
                        "colon:'a:b'"
                ),
                Arguments.of(
                        "'a:b':value",
                        "'a:b':value"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parseData")
    public void parseTest(String input, String expected) {
        String result = parse(input).asValue(true);
        assertEquals(
                expected,
                result
        );
        assertEquals(
                expected,
                parse(result).asValue(true),
                "Double-parsing input lead to another result"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"k:\\", "k:'unterminated", "k:[a", "k:'x'y", "k"})
    public void malformedTest(String input) {
        assertThrows(IllegalArgumentException.class, () -> parse(input));
    }

    @Test
    public void manualTesting() {
        String[] examples = {
                "simple:value",
                "simple:'value'",
                "not-a:'{list}'",
                "not-a:\\{list\\}",
                "first:value second:value",
                "escaping:\\'value\\'",
                "spaces:'value with spaces'",
                "spaces:value\\ with\\ spaces",
                "list:[list of values]",
                "list:['list' 'of' 'values']",
                "list:['singleton list']",
                "map:{subkey1:value subkey2:value}",
                "map:{subkey1:['list' 'of' 'values'] subkey2:value}",
                "list:[list with {key:value}]",
                "deep:{a:[1 {b:[2 3]} 4] c:d}"
        };

        for (String ex : examples) {
            IO.println(ex);
            IO.println("========================================");
            var params = parse(ex);
            String result = params.asValue(true);
            IO.println("views: " + params.view());
            IO.println("tostr: " + result);
            IO.println("parse: " + parse(result).asValue(true));
            IO.println();
        }
    }
}
