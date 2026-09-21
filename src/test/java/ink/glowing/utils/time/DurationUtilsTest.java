package ink.glowing.utils.time;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DurationUtilsTest {
    @ParameterizedTest
    @CsvSource({
            "5, 5000",
            "5s, 5000",
            "5ms, 5",
            "1m, 60000",
            "2h, 7200000",
            "1d, 86400000",
            "'1h 30m', 5400000",
            "'  1m   30s ', 90000"
    })
    public void testParse(String input, long expectedMillis) {
        assertEquals(Duration.ofMillis(expectedMillis), DurationUtils.parseDuration(input));
    }

    @Test
    public void testCustomUnits() {
        Map<String, TemporalUnit> units = Map.of("sec", ChronoUnit.SECONDS, "min", ChronoUnit.MINUTES);
        assertEquals(Duration.ofSeconds(90), DurationUtils.parseDuration("1min 30sec", units));
        assertThrows(IllegalArgumentException.class, () -> DurationUtils.parseDuration("1m", units));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "s", "-1s", "1.5s", "1x", "1 x", "1sm"})
    public void testParseInvalid(String input) {
        assertThrows(IllegalArgumentException.class, () -> DurationUtils.parseDuration(input));
    }
}
