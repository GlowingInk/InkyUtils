package ink.glowing.utils.params;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.stream.Stream;

import static ink.glowing.utils.params.Parameter.parseMap;
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
        String result = parseMap(input).serialize(true);
        assertEquals(
                expected,
                result
        );
        assertEquals(
                expected,
                parseMap(result).serialize(true),
                "Double-parsing input lead to another result"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"k:\\", "k:'unterminated", "k:[a", "k:'x'y", "k", "a :b", "'a' :b", "k:a :b", "k:'a' :b"})
    public void malformedTest(String input) {
        assertThrows(IllegalArgumentException.class, () -> parseMap(input));
    }

    @Test
    public void rawTest() {
        String input = "  k:[a 'b c'  d\\ e ] j:{x:'y z' w:v} q:'quoted' s:esc\\'d n:a:b:c z:[]  ";
        Parameter params = parseMap(input);

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

        Parameter escaped = params.get("s");
        assertEquals("esc'd", escaped.textValue());
    }

    @Test
    public void plainTextValueTest() {
        Parameter params = parseMap("q:'quoted' s:esc\\'d b:bare e:''");

        assertEquals("quoted", params.get("q").textValue());
        assertEquals("esc'd", params.get("s").textValue());
        assertEquals("bare", params.get("b").textValue());
        assertEquals("", params.get("e").textValue());
    }

    @Test
    public void compoundTextValueTest() {
        Parameter params = parseMap("k:[a\\ b 'c d'] j:{x:esc\\'d} e:[]");

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
        assertEquals("'a b'", Parameter.ofValue("a b").raw());
        assertEquals("a", Parameter.ofValue("a").raw());
    }

    @Test
    public void nestingLimitTest() {
        int limit = ParserImpl.MAX_DEPTH;
        assertEquals(1, Parameter.parseList("[".repeat(limit - 1) + "]".repeat(limit - 1)).count());
        assertThrows(IllegalArgumentException.class, () -> Parameter.parseList("[".repeat(limit + 1) + "]".repeat(limit + 1)));
        assertThrows(IllegalArgumentException.class, () -> Parameter.parseList("[".repeat(100_000)));
        assertThrows(IllegalArgumentException.class, () -> parseMap("a:".repeat(limit + 1) + "b"));
    }

    @Test
    public void missingTest() {
        Parameter params = parseMap("k:[a b] p:v e:''");
        Parameter missing = Parameter.missing();

        assertSame(missing, params.get("nope"));
        assertSame(missing, params.get("k").get(5));
        assertSame(missing, params.get("k").get("x"));
        assertSame(missing, params.get("p").get(1));
        assertSame(missing, missing.get("deeper").get(0));
        assertFalse(params.get("e").isMissing(), "Empty plain is not missing");
        assertTrue(params.getMapped("nope", Parameter::isMissing));
        assertEquals(7, params.get("nope").asInt(7));

        assertEquals(0, missing.count());
        assertEquals("", missing.raw());
        assertEquals("", missing.serialize(true));
        assertTrue(missing.matches(missing));
        assertFalse(missing.matches(Parameter.ofValue("")));
        assertFalse(Parameter.ofValue("").matches(missing));
    }

    @Test
    public void findTest() {
        Parameter params = parseMap("k:[a b] e:''");

        assertTrue(params.isMap() && !params.isList());
        assertEquals(List.of("k", "e"), List.copyOf(params.keys()));
        assertTrue(params.get("k").isList() && !params.get("k").isMap());
        assertEquals("a", params.find("k").orElseThrow().get(0).textValue());
        assertEquals("", params.find("e").orElseThrow().textValue(), "Empty plain is present");
        assertTrue(params.find("nope").isEmpty());
        assertEquals("b", params.get("k").find(1).orElseThrow().textValue());
        assertTrue(params.get("k").find(2).isEmpty());
    }

    @Test
    public void absentSkippedInOfTest() {
        Parameter a = Parameter.ofValue("a");
        Parameter missing = Parameter.missing();

        Parameter listed = Parameter.ofList(Arrays.asList(missing, a, null));
        assertEquals(1, listed.count());
        assertSame(a, listed.get(0));
        assertEquals(0, Parameter.ofList(Collections.singletonList(null)).count());

        Map<String, Parameter> map = new LinkedHashMap<>();
        map.put("x", missing);
        map.put("y", a);
        map.put("z", null);
        Parameter mapped = Parameter.ofMap(map);
        assertEquals(1, mapped.count());
        assertSame(a, mapped.get("y"));
        assertSame(missing, mapped.get("x"));
        assertEquals(0, Parameter.ofMap(Collections.singletonMap("x", null)).count());
    }

    @Test
    public void duplicateKeysTest() {
        assertEquals("a:2", parseMap("a:1 A:2").serialize(true));
    }

    @Test
    public void mappedOfTest() {
        Map<String, Parameter> entries = new LinkedHashMap<>();
        for (String key : List.of("one", "two", "three", "four", "five", "six")) {
            entries.put(key, Parameter.ofValue(key));
        }
        Parameter params = Parameter.ofMap(entries);

        assertEquals("one:one two:two three:three four:four five:five six:six", params.serialize(true));
        assertEquals("three", params.get("THREE").textValue());
    }

    @Test
    public void equalsTest() {
        Parameter first = parseMap("k:[a 'b c'] j:{x:y}");
        Parameter same = parseMap( "k:[a 'b c'] j:{x:y}");

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals(first.get("k"), same.get("k"));
        assertEquals(first.get("k").hashCode(), same.get("k").hashCode());

        // Different raw values are not equal, even if they hold the same values
        assertNotEquals(first, parseMap("k:[a  'b c'] j:{x:y}"));
        assertNotEquals(parseMap("k:a").get("k"), parseMap("k:'a'").get("k"));

        // Different kinds are not equal
        assertNotEquals(parseMap("k:a").get("k"), Parameter.parseList("a"));
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
        assertEquals(expected, parseMap(left).matches(parseMap(right)));
        assertEquals(expected, parseMap(right).matches(parseMap(left)), "matches must be symmetric");
    }

    // @Test
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
            var params = parseMap(ex);
            String result = params.serialize(true);
            IO.println("raw  : " + params.raw());
            IO.println("value: " + params.textValue());
            IO.println("tostr: " + result);
            IO.println("parse: " + parseMap(result).serialize(true));
            IO.println();
        }

        IO.println(parseMap("simple:[list of values]").get("simple").textValue());
    }
}
