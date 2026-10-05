package ink.glowing.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * The result of a comparison as a constant, instead of the sign of an {@code int}.
 */
public enum Comparison {
    BELOW,
    EQUAL,
    ABOVE;

    private static final Comparison[] VALUES = values();

    private final int signum;

    Comparison() {
        this.signum = ordinal() - 1;
    }

    /**
     * Returns the sign of this comparison.
     * @return {@code -1} for {@link #BELOW}, {@code 0} for {@link #EQUAL} and {@code 1} for {@link #ABOVE}
     */
    @Contract(pure = true)
    public int signum() {
        return signum;
    }

    /**
     * Returns the comparison for the result of {@link Comparable#compareTo(Object)} or
     * {@link Comparator#compare(Object, Object)}.
     * @param signum the result, only its sign matters
     * @return {@link #BELOW} for a negative result, {@link #ABOVE} for a positive one, otherwise {@link #EQUAL}
     */
    @Contract(pure = true)
    public static @NotNull Comparison ofSignum(int signum) {
        return VALUES[Integer.signum(signum) + 1];
    }

    /**
     * Compares a value to another one by its natural order.
     * @param <T> the type of the values
     * @param tested the value to test
     * @param against the value to compare with
     * @return how {@code tested} compares to {@code against}
     */
    public static <T> @NotNull Comparison of(@NotNull Comparable<? super T> tested, @NotNull T against) {
        return ofSignum(tested.compareTo(against));
    }

    /**
     * Compares a value to another one with a comparator.
     * @param <T> the type of the values
     * @param comparator the comparator
     * @param tested the value to test
     * @param against the value to compare with
     * @return how {@code tested} compares to {@code against}
     */
    public static <T> @NotNull Comparison of(@NotNull Comparator<? super T> comparator, @NotNull T tested, @NotNull T against) {
        return ofSignum(comparator.compare(tested, against));
    }

    /**
     * Checks how a value compares to another one by its natural order, reading as "{@code tested} is
     * {@code expected} {@code against}".
     * @param <T> the type of the values
     * @param tested the value to test
     * @param expected the expected comparison
     * @param against the value to compare with
     * @return {@code true} if {@code tested} compares to {@code against} as expected
     */
    public static <T> boolean is(@NotNull Comparable<? super T> tested, @NotNull Comparison expected, @NotNull T against) {
        return of(tested, against) == expected;
    }

    /**
     * Checks how a value compares to another one with a comparator.
     * @param <T> the type of the values
     * @param comparator the comparator
     * @param tested the value to test
     * @param expected the expected comparison
     * @param against the value to compare with
     * @return {@code true} if {@code tested} compares to {@code against} as expected
     */
    public static <T> boolean is(@NotNull Comparator<? super T> comparator, @NotNull T tested, @NotNull Comparison expected, @NotNull T against) {
        return of(comparator, tested, against) == expected;
    }
}
