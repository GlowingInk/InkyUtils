package ink.glowing.utils.time;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DurationUtilsTest {
    static Stream<Arguments> parseData() {
        return Stream.of(
                Arguments.of("5",Duration.ofSeconds(5)),
                Arguments.of("5s", Duration.ofSeconds(5)),
                Arguments.of("5ms", Duration.ofMillis(5)),
                Arguments.of("1m", Duration.ofMinutes(1)),
                Arguments.of("2h", Duration.ofHours(2)),
                Arguments.of("1d", Duration.ofDays(1)),
                Arguments.of("1h 30m", Duration.ofMinutes(90)),
                Arguments.of("  1m   30s ", Duration.ofSeconds(90)),
                // Arabic 7d, Devanagari 5h, Gurmukhi 3m, Thai 5s
                Arguments.of("٧d ५h ੩m ๕s", Duration.ofDays(7).plusHours(5).plusMinutes(3).plusSeconds(5))
        );
    }

    @ParameterizedTest
    @MethodSource("parseData")
    public void testParse(String input, Duration expected) {
        assertEquals(expected, DurationUtils.parseDuration(input));
    }

    @Test
    public void testCustomUnits() {
        Map<String, TemporalUnit> units = Map.of("sec", ChronoUnit.SECONDS, "min", ChronoUnit.MINUTES, "", ChronoUnit.HOURS);
        assertEquals(
                Duration.ofHours(1).plus(Duration.ofMinutes(1)).plus(Duration.ofSeconds(30)),
                DurationUtils.parseDuration("1 1min 30sec", units)
        );
        assertThrows(IllegalArgumentException.class, () -> DurationUtils.parseDuration("1m", units));
    }

    @ParameterizedTest
    @ValueSource(strings = {"s", "-1s", "1.5s", "1x", "1 x", "1sm"})
    public void testParseInvalid(String input) {
        assertThrows(IllegalArgumentException.class, () -> DurationUtils.parseDuration(input));
    }
}
