package ink.glowing.utils.rng;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class RngUtilsTest {
    private static final int SAMPLES = 1_000;

    private static void assertInRange(double value, int min, int max) {
        assertTrue(value >= min && value < max, value + " not in [" + min + ", " + max + ")");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 10",
            "10, 0",
            "-5, 5",
            "5, -5",
            "-10, -1"
    })
    public void testInRange(int a, int b) {
        int min = Math.min(a, b), max = Math.max(a, b);
        for (int i = 0; i < SAMPLES; i++) {
            assertInRange(RngUtils.inRange(a, b), min, max);
            assertInRange(RngUtils.inRange((long) a, b), min, max);
            assertInRange(RngUtils.inRange((double) a, b), min, max);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {3, -2, 0})
    public void testInRangeEqualBounds(int bound) {
        assertEquals(bound, RngUtils.inRange(bound, bound));
        assertEquals(bound, RngUtils.inRange((long) bound, bound));
        assertEquals(bound, RngUtils.inRange((double) bound, bound));
    }

    @ParameterizedTest
    @CsvSource({
            "NaN, 1",
            "1, NaN",
            "NaN, NaN"
    })
    public void testInRangeNaN(double a, double b) {
        assertThrows(IllegalArgumentException.class, () -> RngUtils.inRange(a, b));
    }
}
