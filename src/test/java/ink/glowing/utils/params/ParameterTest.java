package ink.glowing.utils.params;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ink.glowing.utils.params.Parameter.Mapped.parse;
import static org.junit.jupiter.api.Assertions.*;

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
        String result = parse(input).serialize(true);
        assertEquals(
                expected,
                result
        );
        assertEquals(
                expected,
                parse(result).serialize(true),
                "Double-parsing input lead to another result"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"k:\\", "k:'unterminated", "k:[a", "k:'x'y", "k", "a :b", "'a' :b", "k:a :b", "k:'a' :b"})
    public void malformedTest(String input) {
        assertThrows(IllegalArgumentException.class, () -> parse(input));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void rawTest() {
        String input = "  k:[a 'b c'  d\\ e ] j:{x:'y z' w:v} q:'quoted' s:esc\\'d n:a:b:c z:[]  ";
        Parameter.Mapped params = parse(input);

        assertEquals(input, params.raw());
        assertEquals("[a 'b c'  d\\ e ]", params.get("k").raw());
        assertEquals("a", params.get("k").get(0).raw());
        assertEquals("'b c'", params.get("k").get(1).raw());
        assertEquals("d\\ e", params.get("k").get(2).raw());
        assertEquals("{x:'y z' w:v}", params.get("j").raw());
        assertEquals("'y z'", params.get("j").get("x").raw());
        assertEquals("v", params.get("j").get("w").raw());
        assertEquals("'quoted'", params.get("q").raw());
        assertEquals("esc\\'d", params.get("s").raw());
        assertEquals("a:b:c", params.get("n").raw());
        assertEquals("[]", params.get("z").raw());

        Parameter.Plain escaped = (Parameter.Plain) params.get("s");
        assertEquals("esc'd", escaped.value());
    }

    @Test
    public void valueTest() {
        Parameter.Mapped params = parse("k:[a\\ b 'c d'] j:{x:esc\\'d} q:'quoted' s:esc\\'d");

        assertEquals("k:[a b 'c d'] j:{x:esc'd} q:'quoted' s:esc'd", params.value());
        assertEquals("[a b 'c d']", params.get("k").value());
        assertEquals("{x:esc'd}", params.get("j").value());
        assertEquals("quoted", params.get("q").value());
        assertEquals("esc'd", params.get("s").value());

        // The raw values are not affected
        assertEquals("[a\\ b 'c d']", params.get("k").raw());
    }

    @Test
    public void rawOfBuiltParameterTest() {
        assertEquals("'a b'", Parameter.Plain.of("a b").raw());
        assertEquals("a", Parameter.Plain.of("a").raw());
    }

    @Test
    public void nestingLimitTest() {
        int limit = ParserImpl.MAX_DEPTH;
        assertEquals(1, Parameter.Listed.parse("[".repeat(limit - 1) + "]".repeat(limit - 1)).count());
        assertThrows(IllegalArgumentException.class, () -> Parameter.Listed.parse("[".repeat(limit + 1) + "]".repeat(limit + 1)));
        assertThrows(IllegalArgumentException.class, () -> Parameter.Listed.parse("[".repeat(100_000)));
        assertThrows(IllegalArgumentException.class, () -> parse("a:".repeat(limit + 1) + "b"));
    }

    @Test
    public void caseInsensitiveKeysTest() {
        Parameter.Mapped params = parse("Braced:{Key:v} single:Key:v");

        assertEquals("v", ((Parameter.Plain) params.get("BRACED").get("key")).value());
        assertEquals("v", ((Parameter.Plain) params.get("SINGLE").get("KEY")).value());
        assertNull(params.get("other"));
    }

    @Test
    public void duplicateKeysTest() {
        assertEquals("a:2", parse("a:1 A:2").serialize(true));
    }

    @Test
    public void mappedOfTest() {
        Map<String, Parameter> entries = new LinkedHashMap<>();
        for (String key : List.of("one", "two", "three", "four", "five", "six")) {
            entries.put(key, Parameter.Plain.of(key));
        }
        Parameter.Mapped params = Parameter.Mapped.of(entries);

        assertEquals("one:one two:two three:three four:four five:five six:six", params.serialize(true));
        assertEquals("three", ((Parameter.Plain) params.get("THREE")).value());
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
            String result = params.serialize(true);
            IO.println("raw  : " + params.raw());
            IO.println("value: " + params.value());
            IO.println("tostr: " + result);
            IO.println("parse: " + parse(result).serialize(true));
            IO.println();
        }
    }
}
