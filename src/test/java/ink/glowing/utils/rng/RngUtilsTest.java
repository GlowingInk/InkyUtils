package ink.glowing.utils.rng;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class RngUtilsTest {
    private static final int SAMPLES = 1_000;

    private static void assertInRange(double value, int min, int max) {
        assertTrue(value >= min && value < max, value + " not in [" + min + ", " + max + ")");
    }

    @DataProvider
    public Object[][] rangeData() {
        return new Object[][]{
                {0, 10},
                {10, 0},
                {-5, 5},
                {5, -5},
                {-10, -1}
        };
    }

    @Test(dataProvider = "rangeData")
    public void testInRange(int a, int b) {
        int min = Math.min(a, b), max = Math.max(a, b);
        for (int i = 0; i < SAMPLES; i++) {
            assertInRange(RngUtils.inRange(a, b), min, max);
            assertInRange(RngUtils.inRange((long) a, b), min, max);
            assertInRange(RngUtils.inRange((double) a, b), min, max);
        }
    }

    @DataProvider
    public Object[][] equalBoundsData() {
        return new Object[][]{{3}, {-2}, {0}};
    }

    @Test(dataProvider = "equalBoundsData")
    public void testInRangeEqualBounds(int bound) {
        assertEquals(RngUtils.inRange(bound, bound), bound);
        assertEquals(RngUtils.inRange((long) bound, bound), bound);
        assertEquals(RngUtils.inRange((double) bound, bound), bound);
    }

    @DataProvider
    public Object[][] nanRangeData() {
        return new Object[][]{
                {Double.NaN, 1d},
                {1d, Double.NaN},
                {Double.NaN, Double.NaN}
        };
    }

    @Test(dataProvider = "nanRangeData")
    public void testInRangeNaN(double a, double b) {
        assertThrows(IllegalArgumentException.class, () -> RngUtils.inRange(a, b));
    }
}
