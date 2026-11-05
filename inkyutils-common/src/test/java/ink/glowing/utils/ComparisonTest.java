package ink.glowing.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public final class ComparisonTest {
    private static Stream<Arguments> dataOfSignum() {
        return Stream.of(
                arguments(-1, Comparison.BELOW),
                arguments(-100, Comparison.BELOW),
                arguments(0, Comparison.EQUAL),
                arguments(1, Comparison.ABOVE),
                arguments(100, Comparison.ABOVE),
                arguments(Integer.MIN_VALUE, Comparison.BELOW),
                arguments(Integer.MAX_VALUE, Comparison.ABOVE)
        );
    }

    @ParameterizedTest
    @MethodSource("dataOfSignum")
    void ofSignumTest(int signum, Comparison expected) {
        assertEquals(expected, Comparison.ofSignum(signum));
    }

    private static Stream<Arguments> dataOfComparable() {
        return Stream.of(
                arguments(5, 10, Comparison.BELOW),
                arguments(10, 5, Comparison.ABOVE),
                arguments(7, 7, Comparison.EQUAL)
        );
    }

    @ParameterizedTest
    @MethodSource("dataOfComparable")
    void ofComparableTest(Integer left, Integer right, Comparison expected) {
        assertEquals(expected, Comparison.of(left, right));
        for (Comparison other : Comparison.values()) {
            assertEquals(other == expected, Comparison.is(left, other, right));
        }
    }

    private static Stream<Arguments> dataOfComparator() {
        return Stream.of(
                arguments("hey", "hello", Comparison.BELOW),
                arguments("hello", "hey", Comparison.ABOVE),
                arguments("test", "code", Comparison.EQUAL)
        );
    }

    @ParameterizedTest
    @MethodSource("dataOfComparator")
    void ofComparatorTest(String left, String right, Comparison expected) {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        assertEquals(expected, Comparison.of(lengthComparator, left, right));
        assertTrue(Comparison.is(lengthComparator, left, expected, right));
    }
}
