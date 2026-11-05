package ink.glowing.utils.num;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class NumberUtilsTest {
    private static Stream<Arguments> parseNumberData() {
        return Stream.of(
                arguments("12", 12),
                arguments("-12", -12),
                arguments("+7", 7),
                arguments("9999999999", 9999999999L),
                arguments("-2147483648", Integer.MIN_VALUE),
                arguments("2147483648", 2147483648L),
                arguments("-9223372036854775808", Long.MIN_VALUE),
                arguments("9223372036854775807", Long.MAX_VALUE),
                arguments("9223372036854775808", 9223372036854775808.0),
                arguments("99999999999999999999", 1e20),
                arguments("6.5", 6.5),
                arguments("1e3", 1000.0),
                arguments("-1.5E-3", -0.0015),
                arguments("1e999", Double.POSITIVE_INFINITY)
        );
    }

    @ParameterizedTest
    @MethodSource("parseNumberData")
    public void testParseNumber(String input, Number expected) {
        assertEquals(Optional.of(expected), NumberUtils.parseNumber(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"+", "-", ".", "e", " 1", "1 ", "1-", "1e3e", "NaN", "Infinity", "0x10", "1L", "1f", "1,5", "abc"})
    public void testParseNumberInvalid(String input) {
        assertTrue(NumberUtils.parseNumber(input).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0.0, 0",
            "0, -0.0, 0",
            "5, 5.0, 0",
            "1, 0.5, 1",
            "0, 0.5, -1",
            "0, -0.5, 1",
            "-1, -0.5, -1",
            "9007199254740992, 9007199254740992.0, 0",
            "9007199254740993, 9007199254740992.0, 1",
            "9007199254740991, 9007199254740992.0, -1",
            "9223372036854775807, 9.223372036854775807E18, -1",
            "-9223372036854775808, -9.223372036854775808E18, 0",
            "-9223372036854775808, -Infinity, 1",
            "9223372036854775807, Infinity, -1",
            "0, NaN, -1",
            "9223372036854775807, NaN, -1"
    })
    public void testCompare(long tested, double against, int expected) {
        assertEquals(expected, Integer.signum(NumberUtils.compare(tested, against)));
    }
}
