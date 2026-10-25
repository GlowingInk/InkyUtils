package ink.glowing.utils.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class HashListTest {
    private static String[] stringArr(String... arr) {
        return arr;
    }
    
    public static Stream<Arguments> hashListData() {
        return Stream.of(
                arguments(stringArr(), 0),
                arguments(stringArr("a"), 1),
                arguments(stringArr("a", "b", "c"), 3),
                arguments(stringArr("a", "b", "a"), 3)
        );
    }

    @ParameterizedTest
    @MethodSource("hashListData")
    public void testSize(String[] input, int expectedSize) {
        HashList<String> list = HashList.of(input);
        assertEquals(expectedSize, list.size());
    }

    public static Stream<Arguments> containsData() {
        return Stream.of(
                arguments(stringArr(), "x", false),
                arguments(stringArr("a"), "a", true),
                arguments(stringArr("a"), "b", false),
                arguments(stringArr("a", "b", "c"), "b", true),
                arguments(stringArr("a", "b", "c"), "d", false),
                arguments(stringArr("a", "b", "a"), "a", true),
                arguments(stringArr("a", "b", "a"), "c", false)
        );
    }

    @ParameterizedTest
    @MethodSource("containsData")
    public void testContains(String[] input, String query, boolean expected) {
        HashList<String> list = HashList.of(input);
        assertEquals(expected, list.contains(query),
                String.format("contains('%s') should be %b", query, expected));
    }

    public static Stream<Arguments> indexOfData() {
        return Stream.of(
                arguments(stringArr(), "x", -1, -1),
                arguments(stringArr("a"), "a", 0, 0),
                arguments(stringArr("a"), "b", -1, -1),
                arguments(stringArr("a", "b", "c"), "c", 2, 2),
                arguments(stringArr("a", "b", "a"), "a", 0, 2),
                arguments(stringArr("a", "b", "a"), "b", 1, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("indexOfData")
    public void testIndexOf(String[] input, String query, int expectedFirst, int expectedLast) {
        HashList<String> list = HashList.of(input);
        assertEquals(expectedFirst, list.indexOf(query), "indexOf");
        assertEquals(expectedLast, list.lastIndexOf(query), "lastIndexOf");
    }

    public static Stream<Arguments> getAndToArrayData() {
        return Stream.of(
                arguments(stringArr(), stringArr()),
                arguments(stringArr("a"), stringArr("a")),
                arguments(stringArr("a", "b"), stringArr("a", "b")),
                arguments(stringArr("a", "b", "c"), stringArr("a", "b", "c")),
                arguments(stringArr("a", "b", "a"), stringArr("a", "b", "a"))
        );
    }

    @ParameterizedTest
    @MethodSource("getAndToArrayData")
    public void testGetAndToArray(String[] input, String[] expectedArray) {
        HashList<String> list = HashList.of(input);
        assertArrayEquals(expectedArray, list.toArray());

        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(expectedArray[i], list.get(i));
        }
    }

    public static Stream<Arguments> nullBehaviorData() {
        return Stream.of(
                arguments(stringArr("a", "b"), -1, -1),
                arguments(stringArr(), -1, -1),
                arguments(stringArr("a", null, "b"), 1, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("nullBehaviorData")
    public void testNullBehavior(String[] input, int expectedIndexOfNull, int expectedLastIndexOfNull) {
        HashList<String> list = HashList.of(input);

        boolean inputContainsNull = false;
        for (String s : input) {
            if (s == null) {
                inputContainsNull = true;
                break;
            }
        }

        assertEquals(inputContainsNull, list.contains(null));
        assertEquals(expectedIndexOfNull, list.indexOf(null));
        assertEquals(expectedLastIndexOfNull, list.lastIndexOf(null));
    }

    @Test
    public void testCustomStrategy() {
        var composer = new HashList.Composer<String>().containsStrategy(CaseInsensitive.strategy());
        List.of("Alpha", "Beta", "Gamma").forEach(composer::add);
        HashList<String> list = composer.finish();

        assertTrue(list.contains("alpha"), "Should find case-insensitive 'alpha'");
        assertTrue(list.contains("BETA"), "Should find case-insensitive 'BETA'");
        assertFalse(list.contains("delta"), "Should not find 'delta'");

        assertEquals(3, list.size(), "Size should be 3");

        assertEquals("Alpha", list.get(0), "First element should be 'Alpha'");
        assertEquals("Beta", list.get(1), "Second element should be 'Beta'");
        assertEquals("Gamma", list.get(2), "Third element should be 'Gamma'");

        assertEquals(0, list.indexOf("alpha"), "indexOf('alpha') should return 0");
        assertEquals(1, list.indexOf("BETA"), "indexOf('BETA') should return 1");
        assertEquals(2, list.indexOf("gamma"), "indexOf('gamma') should return 2");
    }
}