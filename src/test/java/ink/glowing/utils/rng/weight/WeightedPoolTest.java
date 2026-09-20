package ink.glowing.utils.rng.weight;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.random.RandomGenerator;

import static org.testng.Assert.*;

public class WeightedPoolTest {
    private static final int SAMPLES = 100_000;

    private static RandomGenerator rng() {
        return new Random(1234);
    }

    @Test
    public void testEmpty() {
        assertSame(WeightedPicker.<String>of(), WeightedPicker.<Integer>of());

        WeightedPicker<String> pool = WeightedPicker.of();
        assertTrue(pool.isEmpty());
        assertThrows(NoSuchElementException.class, () -> pool.next(rng()));
    }

    @Test
    public void testSingle() {
        WeightedPicker<String> pool = WeightedPicker.of("a");
        assertFalse(pool.isEmpty());
        assertEquals(pool.next(rng()), "a");
    }

    @DataProvider
    public Object[][] emptyData() {
        return new Object[][]{
                {List.of()},
                {List.of("a")},
                {List.of("a", "b")},
                {List.of("a", "b", "c")}
        };
    }

    @Test(dataProvider = "emptyData")
    public void testNonPositiveWeightsAreEmpty(List<String> input) {
        WeightedPicker<String> zero = WeightedPicker.ofCollection(input, _ -> 0d);
        assertTrue(zero.isEmpty());
        assertThrows(NoSuchElementException.class, () -> zero.next(rng()));

        WeightedPicker<String> negative = WeightedPicker.ofCollection(input, _ -> -1d);
        assertTrue(negative.isEmpty());
    }

    @Test
    public void testInvalidWeightNeverPicked() {
        WeightedPicker<String> pool = WeightedPicker.ofCollection(
                List.of("a", "b", "c", "d"),
                s -> switch (s) {
                    case "b" -> 0;
                    case "c" -> Double.NaN;
                    default -> 1;
                }
        );
        assertFalse(pool.isEmpty());

        RandomGenerator rng = rng();
        for (int i = 0; i < SAMPLES; i++) {
            assertNotEquals(pool.next(rng), "b");
            assertNotEquals(pool.next(rng), "c");
        }
    }

    @DataProvider
    public Object[][] distributionData() {
        return new Object[][]{
                {new double[]{1, 1}},
                {new double[]{1, 3}},
                {new double[]{1, 1, 1, 1}},
                {new double[]{1, 2, 3, 4}},
                {new double[]{10, 1, 0.5, 0.1}},
                {new double[]{0.001, 0.002, 0.003}}
        };
    }

    @Test(dataProvider = "distributionData")
    public void testDistribution(double[] weights) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) indices.add(i);

        WeightedPicker<Integer> pool = WeightedPicker.ofCollection(indices, i -> weights[i]);

        double sum = 0;
        for (double w : weights) sum += w;

        int[] counts = new int[weights.length];
        RandomGenerator rng = rng();
        for (int i = 0; i < SAMPLES; i++) counts[pool.next(rng)]++;

        for (int i = 0; i < weights.length; i++) {
            assertEquals((double) counts[i] / SAMPLES, weights[i] / sum, 0.01, "Frequency of element " + i);
        }
    }

    @Test
    public void testOfMapped() {
        Map<String, Double> map = new HashMap<>();
        map.put("a", 1d);
        map.put("b", 0d);
        map.put("c", 3d);

        WeightedPicker<String> pool = WeightedPicker.ofMapped(map);

        int a = 0, c = 0;
        RandomGenerator rng = rng();
        for (int i = 0; i < SAMPLES; i++) {
            switch (pool.next(rng)) {
                case "a" -> a++;
                case "c" -> c++;
                default -> fail("Unexpected element");
            }
        }
        assertEquals((double) a / SAMPLES, 0.25, 0.01);
        assertEquals((double) c / SAMPLES, 0.75, 0.01);
    }
}
