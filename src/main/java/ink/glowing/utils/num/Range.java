package ink.glowing.utils.num;

import ink.glowing.utils.Matchable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * A set of numbers made of {@linkplain Interval intervals} between two bounds, each either inclusive or exclusive,
 * or {@linkplain Bound#infinite() infinite} to leave that side unbounded.
 * <p>
 * Intervals are made of bounds, like {@code Range.of(Bound.inclusive(1), Bound.exclusive(7))},
 * or parsed from strings like {@code [1, 7)}: a square bracket makes its bound inclusive, a round one
 * exclusive, and an empty bound like in {@code [1, )} leaves its side unbounded.
 * Ranges are combined with {@link #or(Range)} and {@link #and(Range)}, and a result with gaps is
 * a {@link Compound} range written like {@code [1, 3) U (5, 7]}.
 * <p>
 * A range without any number in it, like {@code [7, 1]} or {@code (1, 1)}, is {@link #EMPTY}.
 */
public sealed interface Range extends Matchable<Range> permits Range.Interval, Range.Compound {
    /**
     * The range that contains nothing: one whose bounds leave no number between them or has an invalid bound.
     * @see #isEmpty()
     */
    Interval EMPTY = new RangeImpl.IntervalImpl(Bound.Invalid.INSTANCE, Bound.Invalid.INSTANCE);

    /**
     * The unbounded range, which contains every number but {@link Double#NaN}.
     */
    Interval ALL = new RangeImpl.IntervalImpl(Bound.Infinite.INSTANCE, Bound.Infinite.INSTANCE);

    /**
     * Returns an interval of the bounds.
     * @param min the lower bound
     * @param max the upper bound
     * @return the interval, or {@link #EMPTY} if either bound is invalid or no number is between them
     */
    @Contract(pure = true)
    static @NotNull Interval of(@NotNull Bound min, @NotNull Bound max) {
        return RangeImpl.interval(min, max);
    }

    /**
     * Parses a range like {@code [12, 6.5)}, or a union of intervals like {@code [1, 3) U (5, 7]}.
     * Intervals without numbers in them, like {@code [7, 1]}, are left out of the union.
     * @param str the string to parse
     * @return the parsed range, or {@link #EMPTY} if the string is malformed
     * @see Interval#parse(String)
     */
    @Contract(pure = true)
    static @NotNull Range parse(@NotNull String str) {
        return RangeImpl.parse(str);
    }

    /**
     * Returns whether this range contains no numbers.
     * @return {@code true} if this is {@link #EMPTY}
     */
    @Contract(pure = true)
    boolean isEmpty();

    /**
     * Returns whether the value is in this range.
     * @param value the value to check
     * @return {@code true} if the value is in one of the intervals
     */
    @Contract(pure = true)
    boolean contains(long value);

    /**
     * Returns whether the value is in this range. {@code NaN} is in no range.
     * @param value the value to check
     * @return {@code true} if the value is in one of the intervals
     */
    @Contract(pure = true)
    boolean contains(double value);

    /**
     * Checks whether the other range has the same intervals, regardless of their bound types:
     * {@code [1, 2]} matches {@code [1.0, 2.0]}.
     * @param other the range to compare with
     * @return {@code true} if all intervals match
     * @see Bound#matches(Bound)
     */
    @Override
    @Contract(pure = true)
    boolean matches(@NotNull Range other);

    /**
     * Returns the range of numbers in either range. Overlapping and touching intervals are merged:
     * {@code [1, 3]} or {@code (3, 5]} is {@code [1, 5]}.
     * @param other the range to add
     * @return the union of both ranges
     */
    @Contract(pure = true)
    default @NotNull Range or(@NotNull Range other) {
        return RangeImpl.or(this, other);
    }

    /**
     * Returns the range of numbers in both ranges.
     * @param other the range to intersect with
     * @return the intersection of both ranges
     */
    @Contract(pure = true)
    default @NotNull Range and(@NotNull Range other) {
        return RangeImpl.and(this, other);
    }

    /**
     * A single range of numbers between two bounds.
     * @see Range#of(Bound, Bound)
     */
    sealed interface Interval extends Range permits RangeImpl.IntervalImpl {
        /**
         * Parses an interval like {@code [12, 6.5)}.
         * @param str the string to parse
         * @return the parsed interval, or {@link #EMPTY} if the string is malformed
         * @see Range#parse(String)
         */
        @Contract(pure = true)
        static @NotNull Interval parse(@NotNull String str) {
            Interval interval = RangeImpl.parseInterval(str);
            return interval == null ? EMPTY : interval;
        }

        /**
         * Returns the lower bound.
         * @return the lower bound
         */
        @Contract(pure = true)
        @NotNull Bound min();

        /**
         * Returns the upper bound.
         * @return the upper bound
         */
        @Contract(pure = true)
        @NotNull Bound max();
    }

    /**
     * A range of two or more intervals with gaps between them.
     * @see Range#or(Range)
     */
    sealed interface Compound extends Range permits RangeImpl.CompoundImpl {
        /**
         * Returns the intervals this range is made of, sorted and with gaps between them.
         * @return the intervals, at least two
         */
        @Contract(pure = true)
        @NotNull @Unmodifiable List<Interval> intervals();
    }
}
