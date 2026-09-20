package ink.glowing.utils.rng.weight;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.random.RandomGenerator;

import static org.testng.Assert.*;

public class WeightedPickerTest {
    private static final int SAMPLES = 100_000;

    private static RandomGenerator rng() {
        return new Random(1234);
    }

    private static WeightedPicker<Integer> indexPicker(double... weights) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) indices.add(i);
        return WeightedPicker.ofCollection(indices, i -> weights[i]);
    }

    private static void assertFrequencies(WeightedPicker<Integer> picker, double... expected) {
        int[] counts = new int[expected.length];
        RandomGenerator rng = rng();
        for (int i = 0; i < SAMPLES; i++) counts[picker.next(rng)]++;

        for (int i = 0; i < expected.length; i++) {
            if (expected[i] == 0) {
                assertEquals(counts[i], 0, "Element " + i + " should never be picked");
            } else {
                assertEquals((double) counts[i] / SAMPLES, expected[i], 0.01, "Frequency of element " + i);
            }
        }
    }
    
    private static double[] doubleArr(double... arr) {
        return arr;
    }

    @Test
    public void testEmpty() {
        assertSame(WeightedPicker.<String>of(), WeightedPicker.<Integer>of());

        WeightedPicker<String> picker = WeightedPicker.of();
        assertTrue(picker.isEmpty());
        assertThrows(NoSuchElementException.class, () -> picker.next(rng()));
    }

    @Test
    public void testSingle() {
        WeightedPicker<String> picker = WeightedPicker.of("a");
        assertFalse(picker.isEmpty());
        assertEquals(picker.next(rng()), "a");
    }

    @DataProvider
    public Object[][] emptyData() {
        return new Object[][]{
                {doubleArr()},
                {doubleArr(0)},
                {doubleArr(-1, Double.NaN)},
                {doubleArr(0, 0, -1)}
        };
    }

    @Test(dataProvider = "emptyData")
    public void testNoPositiveWeightsIsEmpty(double[] weights) {
        WeightedPicker<Integer> picker = indexPicker(weights);
        assertTrue(picker.isEmpty());
        assertThrows(NoSuchElementException.class, () -> picker.next(rng()));
    }

    @DataProvider
    public Object[][] distributionData() {
        double inf = Double.POSITIVE_INFINITY;
        return new Object[][]{
                {doubleArr(1, 1), doubleArr(0.5, 0.5)},
                {doubleArr(1, 3), doubleArr(0.25, 0.75)},
                {doubleArr(1, 2, 3, 4), doubleArr(0.1, 0.2, 0.3, 0.4)},
                {doubleArr(0.001, 0.002, 0.003), doubleArr(1d / 6, 2d / 6, 3d / 6)},
                // Non-positive weights are never picked
                {doubleArr(1, 0, Double.NaN, -1, 1), doubleArr(0.5, 0, 0, 0, 0.5)},
                // Infinite weights discard finite ones
                {doubleArr(inf, 1), doubleArr(1, 0)},
                {doubleArr(1, inf, 5, inf, 0), doubleArr(0, 0.5, 0, 0.5, 0)},
                // Weights that overflow their sum
                {doubleArr(Double.MAX_VALUE, Double.MAX_VALUE, 1), doubleArr(0.5, 0.5, 0)}
        };
    }

    @Test(dataProvider = "distributionData")
    public void testDistribution(double[] weights, double[] expected) {
        assertFrequencies(indexPicker(weights), expected);
    }

    @Test
    public void testOfMapped() {
        Map<Integer, Double> map = Map.of(0, 1d, 1, 0d, 2, 3d);
        assertFrequencies(WeightedPicker.ofMapped(map), 0.25, 0, 0.75);

        Object2DoubleMap<Integer> primitiveMap = new Object2DoubleOpenHashMap<>();
        primitiveMap.putAll(map);
        assertFrequencies(WeightedPicker.ofMapped(primitiveMap), 0.25, 0, 0.75);
    }

    @Test
    public void testWildcardTypes() {
        List<Integer> integers = List.of(1, 2);
        WeightedPicker<Number> picker = WeightedPicker.ofCollection(integers, Number::doubleValue);
        assertFalse(picker.isEmpty());
    }

    @Test
    public void testComposer() {
        WeightedPicker<Integer> picker = new WeightedPicker.Composer<Integer>()
                .add(0, 1)
                .add(1, 0)
                .add(2, Double.NaN)
                .add(3, 1)
                .finish();
        assertFrequencies(picker, 0.5, 0, 0, 0.5);
    }

    @Test
    public void testComposerInfiniteWhileAdding() {
        double inf = Double.POSITIVE_INFINITY;
        WeightedPicker<Integer> picker = new WeightedPicker.Composer<Integer>()
                .add(0, 1)
                .add(1, inf)
                .add(2, 1)
                .add(3, inf)
                .finish();
        assertFrequencies(picker, 0, 0.5, 0, 0.5);
    }

    @Test
    public void testComposerAddAll() {
        WeightedPicker<Integer> picker = new WeightedPicker.Composer<Integer>(3)
                .addAll(Map.of(0, 1d))
                .addAll(List.of(1, 2), i -> i == 1 ? 0 : 3)
                .finish();
        assertFrequencies(picker, 0.25, 0, 0.75);
    }

    @Test
    public void testComposerSize() {
        WeightedPicker.Composer<Integer> composer = new WeightedPicker.Composer<>();
        assertTrue(composer.isEmpty());

        composer.add(0, 0).add(1, Double.NaN);
        assertTrue(composer.isEmpty());

        composer.addAll(List.of(2, 3), i -> 1);
        assertFalse(composer.isEmpty());
        assertEquals(composer.size(), 2);

        composer.add(4, Double.POSITIVE_INFINITY);
        assertEquals(composer.size(), 1);
    }
}
