package ink.glowing.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static ink.glowing.utils.TextUtils.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextUtilsTest {
    @ParameterizedTest
    @CsvSource({
            "Foo bar one two three, o, a"
    })
    public void testReplaceEach(String input, String search, String replacement) {
        assertEquals(
                input.replace(search, replacement),
                replaceEach(input, search, _ -> replacement)
        );
    }

    static Stream<Arguments> findEachData() {
        return Stream.of(
                Arguments.of("1a 2a 3a 4a 5a", "a", new int[]{1, 4, 7, 10, 13})
        );
    }

    @ParameterizedTest
    @MethodSource("findEachData")
    public void testFindEach(String input, String search, int[] expectedIndexes) {
        int[] count = {0};
        int[] indexes = new int[expectedIndexes.length];
        findEach(input, search, i -> indexes[count[0]++] = i);
        assertArrayEquals(expectedIndexes, indexes);
    }

    @ParameterizedTest
    @CsvSource({
            "abcd, b, 0, 4, 1",
            "abcd, b, 2, 4, -1",
            "abbc, b, 0, 4, 1",
            "abbc, b, 2, 4, 2",
            "abcd, e, 0, 4, -1",
            "abcd, a, 0, 4, 0",
            "abcd, d, 0, 4, 3",
            "abcd, d, 0, 3, -1",
            "aaaa, a, 0, 4, 0",
            "aaaa, a, 2, 4, 2",
            "'', a, 0, 0, -1",
            "a, a, 0, 1, 0",
            "a, b, 0, 1, -1"
    })
    public void testIndexOf(String arrayStr, char ch, int from, int to, int expected) {
        assertEquals(expected, indexOf(arrayStr.toCharArray(), ch, from, to));
    }

    @ParameterizedTest
    @CsvSource({
            "Hello, 0, 5, Hello",
            "Hello, 1, 4, ell",
            "Hello, 1, 5, ello",
            "Hello, 0, 0, ''",
            "Hello, 2, 2, ''",
            "Hello, 4, 5, o",
            "abc, 0, 3, abc",
            "abc, 1, 2, b",
            "' ', 0, 1, ' '",
            "123, 0, 1, 1"
    })
    public void testSubstring(String srcStr, int start, int end, String expected) {
        assertEquals(expected, substring(srcStr.toCharArray(), start, end));
    }

    @ParameterizedTest
    @CsvSource({
            "abcde, 0, 5, abcde",
            "abcde, 1, 4, bcd",
            "abcde, 2, 5, cde",
            "abcde, 0, 3, abc",
            "abcde, 0, 0, ''",
            "abcde, 4, 5, e",
            "abc, 0, 3, abc",
            "abc, 1, 2, b",
            "x, 0, 1, x",
            "123, 0, 1, 1"
    })
    public void testSubarrayCharArray(String srcStr, int start, int end, String expected) {
        assertArrayEquals(expected.toCharArray(), subarray(srcStr.toCharArray(), start, end));
    }

    @ParameterizedTest
    @CsvSource({
            "Hello, 0, 5, Hello",
            "Hello, 1, 4, ell",
            "Hello, 1, 5, ello",
            "Hello, 0, 0, ''",
            "Hello, 2, 2, ''",
            "Hello, 4, 5, o",
            "abc, 0, 3, abc",
            "abc, 1, 2, b",
            "' ', 0, 1, ' '",
            "123, 0, 1, 1",
            "'', 0, 0, ''",
            "test, 2, 4, st",
            "test, 0, 1, t"
    })
    public void testSubarrayCharSequence(CharSequence src, int start, int end, String expected) {
        assertArrayEquals(expected.toCharArray(), subarray(src, start, end));
    }
}
