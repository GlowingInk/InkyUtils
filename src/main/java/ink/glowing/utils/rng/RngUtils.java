package ink.glowing.utils.rng;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Static helpers for picking random values.
 */
public final class RngUtils {
    private RngUtils() { }

    /**
     * Returns the {@link ThreadLocalRandom} of the current thread.
     * @return the current thread's random generator
     */
    public static @NotNull ThreadLocalRandom threadRandom() {
        return ThreadLocalRandom.current();
    }

    /**
     * Returns a uniformly random element of the specified array.
     * @param <$Type> the type of elements in the array
     * @param array the array to pick from
     * @return a random element of the array
     * @throws IllegalArgumentException if the array is empty
     * @see #threadRandom()
     * @see #randomElement(RandomGenerator, Object[])
     */
    public static <$Type> $Type randomElement($Type @NotNull [] array) {
        return randomElement(threadRandom(), array);
    }

    /**
     * Returns a uniformly random element of the specified array.
     * @param <$Type> the type of elements in the array
     * @param rng the source of randomness
     * @param array the array to pick from
     * @return a random element of the array
     * @throws IllegalArgumentException if the array is empty
     */
    public static <$Type> $Type randomElement(@NotNull RandomGenerator rng, $Type @NotNull [] array) {
        return array[rng.nextInt(array.length)];
    }

    /**
     * Returns a uniformly random element of the specified list.
     * @param <$Type> the type of elements in the list
     * @param list the list to pick from
     * @return a random element of the list
     * @throws IllegalArgumentException if the list is empty
     * @see #threadRandom()
     * @see #randomElement(RandomGenerator, List)
     */
    public static <$Type> $Type randomElement(@NotNull List<$Type> list) {
        return randomElement(threadRandom(), list);
    }

    /**
     * Returns a uniformly random element of the specified list.
     * @param <$Type> the type of elements in the list
     * @param rng the source of randomness
     * @param list the list to pick from
     * @return a random element of the list
     * @throws IllegalArgumentException if the list is empty
     */
    public static <$Type> $Type randomElement(@NotNull RandomGenerator rng, @NotNull List<$Type> list) {
        return list.get(rng.nextInt(list.size()));
    }

    /**
     * Returns a random value between the two bounds, in any order. The lower bound is
     * inclusive and the upper bound is exclusive.
     * @param a one of the bounds
     * @param b the other bound
     * @return a random value in {@code [min(a, b), max(a, b))}
     * @throws IllegalArgumentException if either bound is {@code NaN}
     * @see #threadRandom()
     * @see #inRange(RandomGenerator, double, double)
     */
    public static double inRange(double a, double b) {
        return inRange(threadRandom(), a, b);
    }

    /**
     * Returns a random value between the two bounds, in any order. The lower bound is
     * inclusive and the upper bound is exclusive.
     * @param rng the source of randomness
     * @param a one of the bounds
     * @param b the other bound
     * @return a random value in {@code [min(a, b), max(a, b))}
     * @throws IllegalArgumentException if either bound is {@code NaN}
     */
    public static double inRange(@NotNull RandomGenerator rng, double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) throw new IllegalArgumentException("Bounds must not be NaN");
        if (a == b) return a;
        return (b > a)
                ? rng.nextDouble(a, b)
                : rng.nextDouble(b, a);
    }

    /**
     * Returns a random value between the two bounds, in any order. The lower bound is
     * inclusive and the upper bound is exclusive.
     * @param a one of the bounds
     * @param b the other bound
     * @return a random value in {@code [min(a, b), max(a, b))}
     * @see #threadRandom()
     * @see #inRange(RandomGenerator, long, long)
     */
    public static long inRange(long a, long b) {
        return inRange(threadRandom(), a, b);
    }

    /**
     * Returns a random value between the two bounds, in any order. The lower bound is
     * inclusive and the upper bound is exclusive.
     * @param rng the source of randomness
     * @param a one of the bounds
     * @param b the other bound
     * @return a random value in {@code [min(a, b), max(a, b))}
     */
    public static long inRange(@NotNull RandomGenerator rng, long a, long b) {
        if (a == b) return a;
        return (b > a)
                ? rng.nextLong(a, b)
                : rng.nextLong(b, a);
    }

    /**
     * Returns a random value between the two bounds, in any order. The lower bound is
     * inclusive and the upper bound is exclusive.
     * @param a one of the bounds
     * @param b the other bound
     * @return a random value in {@code [min(a, b), max(a, b))}
     * @see #threadRandom()
     * @see #inRange(RandomGenerator, int, int)
     */
    public static int inRange(int a, int b) {
        return inRange(threadRandom(), a, b);
    }

    /**
     * Returns a random value between the two bounds, in any order. The lower bound is
     * inclusive and the upper bound is exclusive.
     * @param rng the source of randomness
     * @param a one of the bounds
     * @param b the other bound
     * @return a random value in {@code [min(a, b), max(a, b))}
     */
    public static int inRange(@NotNull RandomGenerator rng, int a, int b) {
        if (a == b) return a;
        return (b > a)
                ? rng.nextInt(a, b)
                : rng.nextInt(b, a);
    }

    /**
     * Returns {@code true} with the specified probability.
     * @param probability the probability of returning {@code true}, in {@code [0, 1]}
     * @return {@code true} with the specified probability
     * @see #threadRandom()
     * @see #chance(RandomGenerator, double)
     */
    public static boolean chance(double probability) {
        return chance(threadRandom(), probability);
    }

    /**
     * Returns {@code true} with the specified probability.
     * @param rng the source of randomness
     * @param probability the probability of returning {@code true}, in {@code [0, 1]}
     * @return {@code true} with the specified probability
     */
    public static boolean chance(@NotNull RandomGenerator rng, double probability) {
        return rng.nextDouble() < probability;
    }

    /**
     * Returns {@code true} with the specified percent chance.
     * @param percent the probability of returning {@code true}, in {@code [0, 100]}
     * @return {@code true} with the specified percent chance
     * @see #threadRandom()
     * @see #percentChance(RandomGenerator, double)
     */
    public static boolean percentChance(double percent) {
        return percentChance(threadRandom(), percent);
    }

    /**
     * Returns {@code true} with the specified percent chance.
     * @param rng the source of randomness
     * @param percent the probability of returning {@code true}, in {@code [0, 100]}
     * @return {@code true} with the specified percent chance
     */
    public static boolean percentChance(@NotNull RandomGenerator rng, double percent) {
        return chance(rng, percent / 100.0);
    }
}