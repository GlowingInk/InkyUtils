package ink.glowing.utils.params;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static ink.glowing.utils.params.Parameter.Mapped.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parseData")
    public void parseTest(String input, String expected) {
        String result = parse(input).asParameterValue(true);
        assertEquals(
                expected,
                result
        );
        assertEquals(
                expected,
                parse(result).asParameterValue(true),
                "Double-parsing input lead to another result"
        );
    }

    @Test
    public void manualTesting() {
        String[] examples = {
                "simple:value",
                "simple:'value'",
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
            String result = params.asParameterValue(true);
            IO.println(params.value());
            IO.println(result);
            IO.println(parse(result).asParameterValue(true));
            IO.println();
        }
    }
}
