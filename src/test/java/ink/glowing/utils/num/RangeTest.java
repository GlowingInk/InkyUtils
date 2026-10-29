package ink.glowing.utils.num;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class RangeTest {
    @ParameterizedTest
    @CsvSource({
            "'[1, 7)', '[1, 7)'",
            "'(0, 2.5]', '(0, 2.5]'",
            "' [ -3 ,4 ] ', '[-3, 4]'",
            "'[1e3, 9999999999]', '[1000.0, 9999999999]'",
            "'[1, ]', '[1, )'",
            "'[,]', '(, )'",
            "'[-1e999, 1e999]', '(, )'",
            "'[1, 3) U (5, 7]', '[1, 3) U (5, 7]'",
            "'(5, 7] U [1, 3)', '[1, 3) U (5, 7]'",
            "'[1, 3] U (3, 5]', '[1, 5]'",
            "'[1, 3) U (3, 5]', '[1, 3) U (3, 5]'",
            "'[1, 4] U [2, 3]', '[1, 4]'",
            "'[7, 1] U [1, 2]', '[1, 2]'",
            "'[-1, -0.0] U [0.0, 1]', '[-1, 1]'"
    })
    public void testParse(String input, String expected) {
        assertEquals(expected, Range.parse(input).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",", "1, 2", "[1, 2", "1, 2]", "[1 2]", "[1, 2, 3]", "{1, 2}", "[1, x]", "[0x10, 1]", "[1e999, ]", "[, -1e999]", "[1, 2] U", "[1, 2] U [1, x]"})
    public void testParseInvalid(String input) {
        assertSame(Range.EMPTY, Range.parse(input));
        assertSame(Range.EMPTY, Range.Interval.parse(input));
    }

    private static Stream<Arguments> ofData() {
        return Stream.of(
                arguments(
                        Range.Interval.parse("[1, 7)"),
                        Bound.inclusive(1),
                        Bound.exclusive(7)
                ),
                arguments(
                        Range.Interval.parse("(0.5, 2.5]"),
                        Bound.exclusive(0.5),
                        Bound.inclusive(2.5)
                ),
                arguments(
                        Range.Interval.parse("(, 3]"),
                        Bound.infinite(),
                        Bound.inclusive(3)
                ),
                arguments(
                        Range.Interval.parse("[,]"),
                        Bound.infinite(),
                        Bound.infinite()
                ),
                arguments(
                        Range.ALL,
                        Bound.infinite(),
                        Bound.infinite()
                ),
                arguments(
                        Range.ALL,
                        Bound.inclusive(Double.NEGATIVE_INFINITY),
                        Bound.exclusive(Double.POSITIVE_INFINITY)
                ),
                arguments(
                        Range.ALL,
                        Bound.exclusive(Double.NEGATIVE_INFINITY),
                        Bound.inclusive(Double.POSITIVE_INFINITY)
                ),
                arguments(
                        Range.Interval.parse("[1, 1]"),
                        Bound.inclusive(1),
                        Bound.inclusive(1)
                ),
                arguments(
                        Range.Interval.parse("[9007199254740993, 9007199254740993]"),
                        Bound.inclusive(9007199254740993L),
                        Bound.inclusive(9007199254740993L)
                ),
                arguments(
                        Range.Interval.parse("(1, 2)"),
                        Bound.exclusive(1),
                        Bound.exclusive(2)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("ofData")
    public void testOf(Range expected, Bound min, Bound max) {
        assertEquals(expected, Range.of(min, max));
        assertFalse(Range.of(min, max).isEmpty());
    }

    private static Stream<Arguments> ofEmptyData() {
        return Stream.of(
                arguments(
                        Bound.inclusive(Double.POSITIVE_INFINITY),
                        Bound.infinite()
                ),
                arguments(
                        Bound.infinite(),
                        Bound.exclusive(Double.NEGATIVE_INFINITY)
                ),
                arguments(
                        Bound.inclusive(Double.NaN),
                        Bound.infinite()
                ),
                arguments(
                        Bound.infinite(),
                        Bound.exclusive(Double.NaN)
                ),
                arguments(
                        Bound.inclusive(7),
                        Bound.inclusive(1)
                ),
                arguments(
                        Bound.exclusive(1),
                        Bound.exclusive(1)
                ),
                arguments(
                        Bound.inclusive(1),
                        Bound.exclusive(1.0)
                ),
                arguments(
                        Bound.exclusive(1.0),
                        Bound.exclusive(Math.nextUp(1.0))
                )
        );
    }

    @ParameterizedTest
    @MethodSource("ofEmptyData")
    public void testOfEmpty(Bound min, Bound max) {
        assertSame(Range.EMPTY, Range.of(min, max));
        assertTrue(Range.of(min, max).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "'[1, 7)', 1, true",
            "'[1, 7)', 6, true",
            "'[1, 7)', 7, false",
            "'[1, 7)', 0, false",
            "'(1, 7]', 1, false",
            "'(1, 7]', 7, true",
            "'(0.5, 1.5)', 1, true",
            "'[1, )', 9999999999, true",
            "'(, 0)', -9999999999, true",
            "'[,]', 0, true",
            "'[1, x]', 1, false",
            "'(-1e999, 0]', -9999999999, true",
            "'[1e999, ]', 5, false",
            "'[, -1e999]', 5, false",
            "'[, 9007199254740993)', 9007199254740992, true",
            "'(9007199254740992.0, ]', 9007199254740993, true",
            "'[0, 1e30)', 9223372036854775807, true",
            "'[1, 3) U (5, 7]', 3, false",
            "'[1, 3) U (5, 7]', 6, true",
            "'(-1e30, 0]', -9223372036854775808, true"
    })
    public void testContains(String range, long value, boolean expected) {
        assertEquals(expected, Range.parse(range).contains(value));
        if ((long) (double) value == value) assertEquals(expected, Range.parse(range).contains((double) value));
    }

    @ParameterizedTest
    @CsvSource({
            "'(0, 2.5]', 2.5, true",
            "'(0, 2.5]', 2.6, false",
            "'(0, 2.5]', 0.001, true",
            "'(0, 2.5]', 0, false",
            "'[1, 2]', 1.5, true",
            "'[,]', Infinity, true",
            "'[,]', NaN, false",
            "'[1, 2]', NaN, false",
            "'(, 1e999)', Infinity, true",
            "'[1e999, ]', Infinity, false",
            "'(1e999, ]', Infinity, false"
    })
    public void testContainsDouble(String range, double value, boolean expected) {
        assertEquals(expected, Range.parse(range).contains(value));
    }

    @ParameterizedTest
    @CsvSource({
            "'[1, 2]', '[1, 2]', true",
            "'[1, 2]', '[1.0, 2.0]', true",
            "'[0, 1]', '[-0.0, 1]', true",
            "'[,]', '(,)', true",
            "'[1, x]', '[a, b]', true",
            "'[1, 2]', '[1, 2)', false",
            "'[1, 2]', '(1, 2]', false",
            "'[1, 2]', '[1, 2.5]', false",
            "'[1, ]', '[1, 2]', false",
            "'[1, 2]', '[1, x]', false",
            "'[-1e999, 1e999]', '[,]', true",
            "'[1e999, ]', '[,]', false",
            "'[1e999, ]', '[1, x]', true",
            "'[1, 2] U [3, 4]', '[1.0, 2.0] U [3.0, 4.0]', true",
            "'[1, 2] U [3, 4]', '[1, 2]', false"
    })
    public void testMatches(String left, String right, boolean expected) {
        assertEquals(expected, Range.parse(left).matches(Range.parse(right)));
        assertEquals(expected, Range.parse(right).matches(Range.parse(left)));
    }
}
