package ink.glowing.utils.params;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
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

        Parameter.Value escaped = (Parameter.Value) params.get("s");
        assertEquals("esc'd", escaped.textValue());
    }

    @Test
    public void plainTextValueTest() {
        Parameter.Mapped params = parse("q:'quoted' s:esc\\'d b:bare e:''");

        assertEquals("quoted", params.get("q").textValue());
        assertEquals("esc'd", params.get("s").textValue());
        assertEquals("bare", params.get("b").textValue());
        assertEquals("", params.get("e").textValue());
    }

    @Test
    public void compoundTextValueTest() {
        Parameter.Mapped params = parse("k:[a\\ b 'c d'] j:{x:esc\\'d} e:[]");

        assertEquals("k:[a b 'c d'] j:{x:esc'd} e:[]", params.textValue());
        assertEquals("[a b 'c d']", params.get("k").textValue());
        assertEquals("{x:esc'd}", params.get("j").textValue());
        assertEquals("[]", params.get("e").textValue());

        // The raw and serialized forms are unaffected
        assertEquals("[a\\ b 'c d']", params.get("k").raw());
        assertEquals("['a b' 'c d']", params.get("k").serialize(false));
    }

    @Test
    public void unescapeTest() {
        assertEquals("a b", Parameter.unescape("a\\ b"));
        assertEquals("it's", Parameter.unescape("it\\'s"));
        assertEquals("a\\b", Parameter.unescape("a\\\\b"));
        assertEquals("plain", Parameter.unescape("plain"));
        assertEquals("[a b 'c']", Parameter.unescape("[a\\ b 'c']"));
    }

    @Test
    public void rawOfBuiltParameterTest() {
        assertEquals("'a b'", Parameter.Value.of("a b").raw());
        assertEquals("a", Parameter.Value.of("a").raw());
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
    public void missingTest() {
        Parameter.Mapped params = parse("k:[a b] p:v e:''");
        Parameter missing = Parameter.missing();

        assertSame(missing, params.get("nope"));
        assertSame(missing, params.get("k").get(5));
        assertSame(missing, params.get("k").get("x"));
        assertSame(missing, params.get("p").get(1));
        assertSame(missing, missing.get("deeper").get(0));
        assertFalse(params.get("e").isMissing(), "Empty plain is not missing");
        assertTrue(params.map("nope", Parameter::isMissing));

        assertEquals(0, missing.count());
        assertEquals("", missing.raw());
        assertEquals("", missing.serialize(true));
        assertTrue(missing.matches(missing));
        assertFalse(missing.matches(Parameter.Value.of("")));
        assertFalse(Parameter.Value.of("").matches(missing));
    }

    @Test
    public void findTest() {
        Parameter.Mapped params = parse("k:[a b] e:''");

        assertEquals("a", params.find("k").orElseThrow().get(0).textValue());
        assertEquals("", params.find("e").orElseThrow().textValue(), "Empty plain is present");
        assertTrue(params.find("nope").isEmpty());
        assertEquals("b", params.get("k").find(1).orElseThrow().textValue());
        assertTrue(params.get("k").find(2).isEmpty());
    }

    @Test
    public void missingBecomesEmptyInOfTest() {
        Parameter a = Parameter.Value.of("a");
        Parameter missing = Parameter.missing();
        Parameter empty = Parameter.Value.of("");

        Parameter.Listed listed = Parameter.Listed.of(Arrays.asList(missing, a, null));
        assertEquals(3, listed.count());
        assertSame(empty, listed.get(0));
        assertSame(a, listed.get(1));
        assertSame(empty, listed.get(2));
        assertEquals(1, Parameter.Listed.of(Collections.singletonList(null)).count());

        Map<String, Parameter> map = new LinkedHashMap<>();
        map.put("x", missing);
        map.put("y", a);
        map.put("z", null);
        Parameter.Mapped mapped = Parameter.Mapped.of(map);
        assertEquals(3, mapped.count());
        assertSame(empty, mapped.get("x"));
        assertSame(a, mapped.get("y"));
        assertSame(empty, mapped.get("z"));
        assertEquals(1, Parameter.Mapped.of(Collections.singletonMap("x", null)).count());
    }

    @Test
    public void duplicateKeysTest() {
        assertEquals("a:2", parse("a:1 A:2").serialize(true));
    }

    @Test
    public void mappedOfTest() {
        Map<String, Parameter> entries = new LinkedHashMap<>();
        for (String key : List.of("one", "two", "three", "four", "five", "six")) {
            entries.put(key, Parameter.Value.of(key));
        }
        Parameter.Mapped params = Parameter.Mapped.of(entries);

        assertEquals("one:one two:two three:three four:four five:five six:six", params.serialize(true));
        assertEquals("three", params.get("THREE").textValue());
    }

    @Test
    public void equalsTest() {
        Parameter first = parse("k:[a 'b c'] j:{x:y}");
        Parameter same = parse( "k:[a 'b c'] j:{x:y}");

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals(first.get("k"), same.get("k"));
        assertEquals(first.get("k").hashCode(), same.get("k").hashCode());

        // Different raw values are not equal, even if they hold the same values
        assertNotEquals(first, parse("k:[a  'b c'] j:{x:y}"));
        assertNotEquals(parse("k:a").get("k"), parse("k:'a'").get("k"));

        // Different kinds are not equal
        assertNotEquals(parse("k:a").get("k"), Parameter.Listed.parse("a"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "k:a                | k:a                   | true",
            "k:a                | k:'a'                 | true",
            "k:a\\ b            | k:'a b'               | true",
            "k:a                | K:a                   | true",
            "k:a                | k:b                   | false",
            "k:a                | k:A                   | false",
            "k:a                | j:a                   | false",
            "k:a j:b            | j:b k:a               | true",
            "k:a j:b            | k:a                   | false",
            "k:{x:1 y:2}        | k:{y:2 x:1}           | true",
            "k:{x:1 y:2}        | k:{y:2 x:3}           | false",
            "k:[a b]            | k:[a  b]              | true",
            "k:[a b]            | k:[b a]               | false",
            "k:[a b]            | k:[a b c]             | false",
            "k:[a {x:1 y:2}]    | k:[a {y:2 x:1}]       | true",
            "k:a                | k:[a]                 | false",
            "k:[a]              | k:{0:a}               | false"
    })
    public void matchesTest(String left, String right, boolean expected) {
        assertEquals(expected, parse(left).matches(parse(right)));
        assertEquals(expected, parse(right).matches(parse(left)), "matches must be symmetric");
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
            IO.println("value: " + params.textValue());
            IO.println("tostr: " + result);
            IO.println("parse: " + parse(result).serialize(true));
            IO.println();
        }

        IO.println(parse("simple:[list of values]").get("simple").textValue());
    }
}
