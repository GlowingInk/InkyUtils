package ink.glowing.utils.num;

import ink.glowing.utils.Matchable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A range of numbers between two bounds, each either inclusive or exclusive, or {@linkplain Bound#infinite() infinite}
 * to leave that side unbounded.
 * <p>
 * Ranges are made of bounds, like {@code Range.of(Bound.inclusive(1), Bound.exclusive(7))},
 * or parsed from strings like {@code [1, 7)}: a square bracket makes its bound inclusive, a round one
 * exclusive, and an empty bound like in {@code [1, )} leaves its side unbounded.
 * <p>
 * A range without any number between its bounds, like {@code [7, 1]} or {@code (1, 1)}, is {@link #EMPTY}.
 */
public final class Range implements Matchable<Range> {
    /**
     * The range that contains nothing: one whose bounds leave no number between them or has an invalid bound.
     * @see #isEmpty()
     */
    public static final Range EMPTY = new Range(Bound.Invalid.INSTANCE, Bound.Invalid.INSTANCE);

    /**
     * The unbounded range, which contains every number but {@code NaN}.
     */
    public static final Range ALL = new Range(Bound.Infinite.INSTANCE, Bound.Infinite.INSTANCE);

    private final @NotNull Bound min;
    private final @NotNull Bound max;

    private Range(@NotNull Bound min, @NotNull Bound max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Returns a range of the bounds. A {@code double} bound of the infinity on its own side leaves that side
     * unbounded, and one of the infinity on the opposite side is invalid.
     * @param min the lower bound
     * @param max the upper bound
     * @return the range, or {@link #EMPTY} if either bound is invalid or no number is between them
     */
    @Contract(pure = true)
    public static @NotNull Range of(@NotNull Bound min, @NotNull Bound max) {
        min = normalize(min, false);
        max = normalize(max, true);
        return min.effectiveDouble(false) <= max.effectiveDouble(true) || min.hasLong(false) && max.hasLong(true) && min.effectiveLong(false) <= max.effectiveLong(true)
                ? new Range(min, max)
                : EMPTY;
    }

    private static @NotNull Bound normalize(@NotNull Bound bound, boolean upper) {
        return bound instanceof Bound.OfDouble(double value, boolean _) && Double.isInfinite(value)
                ? (value > 0 == upper ? Bound.Infinite.INSTANCE : Bound.Invalid.INSTANCE)
                : bound;
    }

    /**
     * Parses a range like {@code [12, 6.5)}.
     * @param str the string to parse
     * @return the parsed range, or {@link #EMPTY} if the string is malformed
     */
    @Contract(pure = true)
    public static @NotNull Range parse(@NotNull String str) {
        String range = str.strip();
        int comma = range.indexOf(',');
        if (comma < 0 || comma != range.lastIndexOf(',')) return EMPTY;
        char open = range.charAt(0);
        char close = range.charAt(range.length() - 1);
        if (open != '[' && open != '(' || close != ']' && close != ')') return EMPTY;

        String minText = range.substring(1, comma).strip();
        String maxText = range.substring(comma + 1, range.length() - 1).strip();
        Number min = NumberUtils.parseNumber(minText).orElse(null);
        Number max = NumberUtils.parseNumber(maxText).orElse(null);
        if (min == null && !minText.isEmpty() || max == null && !maxText.isEmpty()) return EMPTY;
        return of(
                min == null ? Bound.infinite() : open == '[' ? Bound.inclusive(min) : Bound.exclusive(min),
                max == null ? Bound.infinite() : close == ']' ? Bound.inclusive(max) : Bound.exclusive(max)
        );
    }

    /**
     * Returns the lower bound.
     * @return the lower bound
     */
    @Contract(pure = true)
    public @NotNull Bound min() {
        return min;
    }

    /**
     * Returns the upper bound.
     * @return the upper bound
     */
    @Contract(pure = true)
    public @NotNull Bound max() {
        return max;
    }

    /**
     * Returns whether this range contains no numbers.
     * @return {@code true} if this is {@link #EMPTY}
     */
    @Contract(pure = true)
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Checks whether the other range has the same bounds, regardless of their types:
     * {@code [1, 2]} matches {@code [1.0, 2.0]}.
     * @param other the range to compare with
     * @return {@code true} if both bounds match
     * @see Bound#matches(Bound)
     */
    @Override
    @Contract(pure = true)
    public boolean matches(@NotNull Range other) {
        return min().matches(other.min()) && max().matches(other.max());
    }

    /**
     * Returns whether the value is in this range.
     * @param value the value to check
     * @return {@code true} if the value is within the bounds
     */
    @Contract(pure = true)
    public boolean contains(long value) {
        return min().admits(value, false) && max().admits(value, true);
    }

    /**
     * Returns whether the value is in this range. {@code NaN} is in no range.
     * @param value the value to check
     * @return {@code true} if the value is within the bounds
     */
    @Contract(pure = true)
    public boolean contains(double value) {
        return min().admits(value, false) && max().admits(value, true);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this || obj instanceof Range other && min.equals(other.min) && max.equals(other.max);
    }

    @Override
    public int hashCode() {
        return 31 * min.hashCode() + max.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return (min.inclusive() ? "[" : "(") + min + ", " + max + (max.inclusive() ? "]" : ")");
    }
}
