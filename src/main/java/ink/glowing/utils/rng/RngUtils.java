package ink.glowing.utils.rng;

import ink.glowing.utils.num.Range;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * Static helpers for picking random values.
 */
public final class RngUtils {
    private static final long MAX_NANO_SECONDS = Long.MAX_VALUE / 1_000_000_000L;

    private RngUtils() { }

    /**
     * Returns the {@link ThreadLocalRandom} of the current thread.
     * @return the current thread's random generator
     */
    @Contract(pure = true)
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
    @SafeVarargs
    public static <$Type> $Type randomElement($Type @NotNull ... array) {
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
    @SafeVarargs
    public static <$Type> $Type randomElement(@NotNull RandomGenerator rng, $Type @NotNull ... array) {
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
     * Adds distinct random elements of the specified collection to another one, like
     * {@link #forRandomElements(Collection, int, Consumer)}.
     * @param <$Type> the type of elements in the collection
     * @param <$Collection> the type of the collection to add to
     * @param from the collection to pick from
     * @param count how many elements to pick
     * @param into the collection to add the picked elements to
     * @return {@code into}
     * @see #threadRandom()
     * @see #randomElements(RandomGenerator, Collection, int, Collection)
     */
    public static <$Type, $Collection extends Collection<? super $Type>> @NotNull $Collection randomElements(@NotNull Collection<? extends $Type> from, int count, @NotNull $Collection into) {
        return randomElements(threadRandom(), from, count, into);
    }

    /**
     * Adds distinct random elements of the specified collection to another one, like
     * {@link #forRandomElements(RandomGenerator, Collection, int, Consumer)}.
     * @param <$Type> the type of elements in the collection
     * @param <$Collection> the type of the collection to add to
     * @param rng the source of randomness
     * @param from the collection to pick from
     * @param count how many elements to pick
     * @param into the collection to add the picked elements to
     * @return {@code into}
     */
    public static <$Type, $Collection extends Collection<? super $Type>> @NotNull $Collection randomElements(@NotNull RandomGenerator rng, @NotNull Collection<? extends $Type> from, int count, @NotNull $Collection into) {
        forRandomElements(rng, from, count, into::add);
        return into;
    }

    /**
     * Passes distinct random elements of the specified collection to the action, each chosen with
     * equal probability. They are passed in the iteration order of the collection. If the collection
     * has no more than {@code count} elements, all of them are passed.
     * @param <$Type> the type of elements in the collection
     * @param from the collection to pick from
     * @param count how many elements to pick
     * @param action the action to pass the picked elements to
     * @see #threadRandom()
     * @see #forRandomElements(RandomGenerator, Collection, int, Consumer)
     */
    public static <$Type> void forRandomElements(@NotNull Collection<? extends $Type> from, int count, @NotNull Consumer<? super $Type> action) {
        forRandomElements(threadRandom(), from, count, action);
    }

    /**
     * Passes distinct random elements of the specified collection to the action, each chosen with
     * equal probability. They are passed in the iteration order of the collection. If the collection
     * has no more than {@code count} elements, all of them are passed.
     * @param <$Type> the type of elements in the collection
     * @param rng the source of randomness
     * @param from the collection to pick from
     * @param count how many elements to pick
     * @param action the action to pass the picked elements to
     */
    public static <$Type> void forRandomElements(@NotNull RandomGenerator rng, @NotNull Collection<? extends $Type> from, int count, @NotNull Consumer<? super $Type> action) {
        int left = from.size();
        for (var iterator = from.iterator(); count > 0 && iterator.hasNext(); left--) {
            $Type element = iterator.next();
            if (rng.nextInt(left) < count) {
                action.accept(element);
                count--;
            }
        }
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
     * Returns a random duration between the two bounds, in any order. The lower bound is
     * inclusive and the upper bound is exclusive.
     * @param a one of the bounds
     * @param b the other bound
     * @return a random duration in {@code [min(a, b), max(a, b))}
     * @throws ArithmeticException if the bounds are too far apart for their difference to fit in a {@link Duration}
     * @see #threadRandom()
     * @see #inRange(RandomGenerator, Duration, Duration)
     */
    public static @NotNull Duration inRange(@NotNull Duration a, @NotNull Duration b) {
        return inRange(threadRandom(), a, b);
    }

    /**
     * Returns a random duration between the two bounds, in any order. The lower bound is
     * inclusive and the upper bound is exclusive.
     * @param rng the source of randomness
     * @param a one of the bounds
     * @param b the other bound
     * @return a random duration in {@code [min(a, b), max(a, b))}
     * @throws ArithmeticException if the bounds are too far apart for their difference to fit in a {@link Duration}
     */
    public static @NotNull Duration inRange(@NotNull RandomGenerator rng, @NotNull Duration a, @NotNull Duration b) {
        int order = a.compareTo(b);
        if (order == 0) return a;
        Duration min = order < 0 ? a : b;
        Duration span = (order < 0 ? b : a).minus(min);
        if (span.getSeconds() < MAX_NANO_SECONDS) return min.plusNanos(rng.nextLong(span.toNanos()));
        // The span's leftover nanos past its last whole second are never picked... but whatever
        return min.plusSeconds(rng.nextLong(span.getSeconds())).plusNanos(rng.nextLong(1_000_000_000L));
    }

    /**
     * Returns a uniformly random {@code int} in the range.
     * @param range the range to pick from
     * @param def the value to return if the range has no {@code int} values
     * @return a random value in the range, or {@code def}
     * @see #threadRandom()
     * @see #inRange(RandomGenerator, Range, int)
     */
    public static int inRange(@NotNull Range range, int def) {
        return inRange(threadRandom(), range, def);
    }

    /**
     * Returns a uniformly random {@code int} in the range.
     * @param rng the source of randomness
     * @param range the range to pick from
     * @param def the value to return if the range has no {@code int} values
     * @return a random value in the range, or {@code def}
     */
    public static int inRange(@NotNull RandomGenerator rng, @NotNull Range range, int def) {
        if (!range.min().hasInt(false) || !range.max().hasInt(true)) return def;
        int min = range.min().effectiveInt(false);
        int max = range.max().effectiveInt(true);
        if (min > max) return def;
        return (int) rng.nextLong(min, max + 1L);
    }

    /**
     * Returns a uniformly random {@code long} in the range.
     * @param range the range to pick from
     * @param def the value to return if the range has no {@code long} values
     * @return a random value in the range, or {@code def}
     * @see #threadRandom()
     * @see #inRange(RandomGenerator, Range, long)
     */
    public static long inRange(@NotNull Range range, long def) {
        return inRange(threadRandom(), range, def);
    }

    /**
     * Returns a uniformly random {@code long} in the range.
     * @param rng the source of randomness
     * @param range the range to pick from
     * @param def the value to return if the range has no {@code long} values
     * @return a random value in the range, or {@code def}
     */
    public static long inRange(@NotNull RandomGenerator rng, @NotNull Range range, long def) {
        if (!range.min().hasLong(false) || !range.max().hasLong(true)) return def;
        long min = range.min().effectiveLong(false);
        long max = range.max().effectiveLong(true);
        if (min > max) return def;
        if (max < Long.MAX_VALUE) return rng.nextLong(min, max + 1);
        if (min > Long.MIN_VALUE) return rng.nextLong(min - 1, max) + 1;
        return rng.nextLong();
    }

    /**
     * Returns a uniformly random {@code double} in the range. The largest value is
     * practically never returned, unless it's the only value in the range.
     * @param range the range to pick from
     * @param def the value to return if the range has no values or is infinitely wide
     * @return a random value in the range, or {@code def}
     * @see #threadRandom()
     * @see #inRange(RandomGenerator, Range, double)
     */
    public static double inRange(@NotNull Range range, double def) {
        return inRange(threadRandom(), range, def);
    }

    /**
     * Returns a uniformly random {@code double} in the range. The largest value is
     * practically never returned, unless it's the only value in the range.
     * @param rng the source of randomness
     * @param range the range to pick from
     * @param def the value to return if the range has no values or is infinitely wide
     * @return a random value in the range, or {@code def}
     */
    public static double inRange(@NotNull RandomGenerator rng, @NotNull Range range, double def) {
        double min = range.min().effectiveDouble(false);
        double max = range.max().effectiveDouble(true);
        if (min == max) return min;
        if (!(min < max) || !Double.isFinite(max - min)) return def;
        return rng.nextDouble(min, max);
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