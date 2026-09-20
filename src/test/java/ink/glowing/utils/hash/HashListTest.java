package ink.glowing.utils.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class HashListTest {
    public static Stream<Arguments> hashListData() {
        return Stream.of(
                Arguments.of(new String[]{}, 0),
                Arguments.of(new String[]{"a"}, 1),
                Arguments.of(new String[]{"a", "b", "c"}, 3),
                Arguments.of(new String[]{"a", "b", "a"}, 3)
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
                Arguments.of(new String[]{}, "x", false),
                Arguments.of(new String[]{"a"}, "a", true),
                Arguments.of(new String[]{"a"}, "b", false),
                Arguments.of(new String[]{"a", "b", "c"}, "b", true),
                Arguments.of(new String[]{"a", "b", "c"}, "d", false),
                Arguments.of(new String[]{"a", "b", "a"}, "a", true),
                Arguments.of(new String[]{"a", "b", "a"}, "c", false)
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
                Arguments.of(new String[]{}, "x", -1),
                Arguments.of(new String[]{"a"}, "a", 0),
                Arguments.of(new String[]{"a"}, "b", -1),
                Arguments.of(new String[]{"a", "b", "c"}, "c", 2),
                Arguments.of(new String[]{"a", "b", "a"}, "a", 0),
                Arguments.of(new String[]{"a", "b", "a"}, "b", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("indexOfData")
    public void testIndexOf(String[] input, String query, int expected) {
        HashList<String> list = HashList.of(input);
        assertEquals(expected, list.indexOf(query));
    }

    public static Stream<Arguments> lastIndexOfData() {
        return Stream.of(
                Arguments.of(new String[]{}, "x", -1),
                Arguments.of(new String[]{"a"}, "a", 0),
                Arguments.of(new String[]{"a"}, "b", -1),
                Arguments.of(new String[]{"a", "b", "c"}, "c", 2),
                Arguments.of(new String[]{"a", "b", "a"}, "a", 2),
                Arguments.of(new String[]{"a", "b", "a"}, "b", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("lastIndexOfData")
    public void testLastIndexOf(String[] input, String query, int expected) {
        HashList<String> list = HashList.of(input);
        assertEquals(expected, list.lastIndexOf(query));
    }

    public static Stream<Arguments> getAndToArrayData() {
        return Stream.of(
                Arguments.of(new String[]{}, new String[]{}),
                Arguments.of(new String[]{"a"}, new String[]{"a"}),
                Arguments.of(new String[]{"a", "b"}, new String[]{"a", "b"}),
                Arguments.of(new String[]{"a", "b", "c"}, new String[]{"a", "b", "c"}),
                Arguments.of(new String[]{"a", "b", "a"}, new String[]{"a", "b", "a"})
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
                Arguments.of(new String[]{"a", "b"}, -1, -1),
                Arguments.of(new String[]{}, -1, -1),
                Arguments.of(new String[]{"a", null, "b"}, 1, 1)
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