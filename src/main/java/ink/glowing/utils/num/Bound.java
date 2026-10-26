package ink.glowing.utils.num;

import ink.glowing.utils.Matchable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * One side of a {@link Range}: a bound value that is either inclusive or exclusive, an {@link Infinite} side,
 * or an {@link Invalid} one that admits nothing.
 * <p>
 * Methods that depend on the side take {@code upper}, which is {@code true} for the upper bound of a range
 * and {@code false} for the lower one.
 */
public sealed interface Bound extends Matchable<Bound> permits Bound.OfLong, Bound.OfDouble, Bound.Infinite, Bound.Invalid {
    /**
     * Returns an inclusive bound of the given value: {@link #inclusive(long)} for an {@code Integer} or a {@code Long}
     * and {@link #inclusive(double)} for anything else.
     * @param value the bound value
     * @return the bound
     */
    @Contract(pure = true)
    static @NotNull Bound inclusive(@NotNull Number value) {
        return of(value, true);
    }

    /**
     * Returns an exclusive bound of the given value: {@link #exclusive(long)} for an {@code Integer} or a {@code Long}
     * and {@link #exclusive(double)} for anything else.
     * @param value the bound value
     * @return the bound
     */
    @Contract(pure = true)
    static @NotNull Bound exclusive(@NotNull Number value) {
        return of(value, false);
    }

    /**
     * Returns a {@code long} bound whose value is in the range.
     * @param value the bound value
     * @return an {@link OfLong}
     */
    @Contract(value = "_ -> new", pure = true)
    static @NotNull Bound inclusive(long value) {
        return new OfLong(value, true);
    }

    /**
     * Returns a {@code long} bound whose value is not in the range.
     * @param value the bound value
     * @return an {@link OfLong}
     */
    @Contract(value = "_ -> new", pure = true)
    static @NotNull Bound exclusive(long value) {
        return new OfLong(value, false);
    }

    /**
     * Returns a {@code double} bound whose value is in the range.
     * @param value the bound value
     * @return an {@link OfDouble}, or {@link Invalid} for {@code NaN}
     */
    @Contract(pure = true)
    static @NotNull Bound inclusive(double value) {
        return of(value, true);
    }

    /**
     * Returns a {@code double} bound whose value is not in the range.
     * @param value the bound value
     * @return an {@link OfDouble}, or {@link Invalid} for {@code NaN}
     */
    @Contract(pure = true)
    static @NotNull Bound exclusive(double value) {
        return of(value, false);
    }

    /**
     * Returns the bound of an unbounded side.
     * @return {@link Infinite#INSTANCE}
     */
    @Contract(pure = true)
    static @NotNull Bound infinite() {
        return Infinite.INSTANCE;
    }

    private static @NotNull Bound of(@NotNull Number value, boolean inclusive) {
        return switch (value) {
            case Integer _, Long _ -> new OfLong(value.longValue(), inclusive);
            default -> of(value.doubleValue(), inclusive);
        };
    }

    private static @NotNull Bound of(double value, boolean inclusive) {
        return Double.isNaN(value) ? Invalid.INSTANCE : new OfDouble(value, inclusive);
    }

    /**
     * Checks whether the other bound has the same value and inclusiveness, regardless of its type:
     * a {@code long} bound matches a {@code double} one of the same value.
     * @param other the bound to compare with
     * @return {@code true} if both bounds are the same
     */
    @Override
    @Contract(pure = true)
    boolean matches(@NotNull Bound other);

    /**
     * Returns whether the bound value itself is in the range. Infinite and invalid bounds are never inclusive.
     * @return {@code true} if the bound is inclusive
     */
    @Contract(pure = true)
    boolean inclusive();

    /**
     * Returns whether the value is on the inner side of this bound.
     * @param value the value to check
     * @param upper whether this is the upper bound
     * @return {@code true} if this bound lets the value in
     */
    @Contract(pure = true)
    boolean admits(long value, boolean upper);

    /**
     * Returns whether the value is on the inner side of this bound. {@code NaN} is never admitted.
     * @param value the value to check
     * @param upper whether this is the upper bound
     * @return {@code true} if this bound lets the value in
     */
    @Contract(pure = true)
    boolean admits(double value, boolean upper);

    /**
     * Returns the bound as an {@code int}, truncated and clamped to its limits. An infinite bound gives the limit
     * on its side, an invalid one the limit on the opposite side.
     * @param upper whether this is the upper bound
     * @return the bound value
     */
    @Contract(pure = true)
    default int toInt(boolean upper) {
        return Math.clamp(toLong(upper), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns the bound as a {@code long}, truncated and clamped to its limits. An infinite bound gives the limit
     * on its side, an invalid one the limit on the opposite side.
     * @param upper whether this is the upper bound
     * @return the bound value
     */
    @Contract(pure = true)
    long toLong(boolean upper);

    /**
     * Returns the bound as a {@code double}, which may be rounded for a large {@code long}. An infinite bound
     * gives the infinity on its side, an invalid one {@code NaN}.
     * @param upper whether this is the upper bound
     * @return the bound value
     */
    @Contract(pure = true)
    double toDouble(boolean upper);

    /**
     * Returns whether any {@code int} is on the inner side of this bound.
     * @param upper whether this is the upper bound
     * @return {@code true} if this bound admits an {@code int}
     */
    @Contract(pure = true)
    default boolean hasInt(boolean upper) {
        if (!hasLong(upper)) return false;
        long effective = effectiveLong(upper);
        return upper ? effective >= Integer.MIN_VALUE : effective <= Integer.MAX_VALUE;
    }

    /**
     * Returns whether any {@code long} is on the inner side of this bound.
     * @param upper whether this is the upper bound
     * @return {@code true} if this bound admits a {@code long}
     */
    @Contract(pure = true)
    boolean hasLong(boolean upper);

    /**
     * Returns the {@code int} closest to the bound on the inner side of this bound, clamped to its limits.
     * Only meaningful if {@link #hasInt(boolean)} is {@code true}.
     * @param upper whether this is the upper bound
     * @return the closest admitted {@code int}
     */
    @Contract(pure = true)
    default int effectiveInt(boolean upper) {
        return Math.clamp(effectiveLong(upper), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns the {@code long} closest to the bound on the inner side of this bound.
     * Only meaningful if {@link #hasLong(boolean)} is {@code true}.
     * @param upper whether this is the upper bound
     * @return the closest admitted {@code long}
     */
    @Contract(pure = true)
    long effectiveLong(boolean upper);

    /**
     * Returns the {@code double} closest to the bound on the inner side of this bound. An infinite bound gives
     * the infinity on its side, an invalid one {@code NaN}.
     * @param upper whether this is the upper bound
     * @return the closest admitted {@code double}
     */
    @Contract(pure = true)
    double effectiveDouble(boolean upper);

    /**
     * A {@code long} bound.
     * @param value the bound value
     * @param inclusive whether the bound value is in the range
     */
    record OfLong(long value, boolean inclusive) implements Bound {
        @Override
        public boolean admits(long value, boolean upper) {
            return upper ? (inclusive ? value <= this.value : value < this.value) : (inclusive ? value >= this.value : value > this.value);
        }

        @Override
        public boolean admits(double value, boolean upper) {
            if (Double.isNaN(value)) return false;
            int comparison = -NumberUtils.compare(this.value, value);
            return upper ? (inclusive ? comparison <= 0 : comparison < 0) : (inclusive ? comparison >= 0 : comparison > 0);
        }

        @Override
        public long toLong(boolean upper) {
            return value;
        }

        @Override
        public double toDouble(boolean upper) {
            return value;
        }

        @Override
        public boolean hasLong(boolean upper) {
            return inclusive || value != (upper ? Long.MIN_VALUE : Long.MAX_VALUE);
        }

        @Override
        public long effectiveLong(boolean upper) {
            return inclusive ? value : upper ? value - 1 : value + 1;
        }

        @Override
        public double effectiveDouble(boolean upper) {
            double rounded = value;
            int comparison = NumberUtils.compare(value, rounded);
            if (comparison == 0 ? inclusive : upper == comparison > 0) return rounded;
            return upper ? Math.nextDown(rounded) : Math.nextUp(rounded);
        }

        @Override
        public boolean matches(@NotNull Bound other) {
            return switch (other) {
                case OfLong bound -> value == bound.value && inclusive == bound.inclusive;
                case OfDouble bound -> bound.matches(this);
                default -> false;
            };
        }

        @Override
        public @NotNull String toString() {
            return Long.toString(value);
        }
    }

    /**
     * A {@code double} bound, never {@code NaN}.
     * @param value the bound value
     * @param inclusive whether the bound value is in the range
     */
    record OfDouble(double value, boolean inclusive) implements Bound {
        /**
         * Creates a {@code double} bound.
         * @throws IllegalArgumentException if the value is {@code NaN}
         * @see Bound#inclusive(double)
         * @see Bound#exclusive(double)
         */
        public OfDouble {
            if (Double.isNaN(value)) throw new IllegalArgumentException("Bound must not be NaN");
        }

        @Override
        public boolean admits(long value, boolean upper) {
            int comparison = NumberUtils.compare(value, this.value);
            return upper ? (inclusive ? comparison <= 0 : comparison < 0) : (inclusive ? comparison >= 0 : comparison > 0);
        }

        @Override
        public boolean admits(double value, boolean upper) {
            return upper ? (inclusive ? value <= this.value : value < this.value) : (inclusive ? value >= this.value : value > this.value);
        }

        @Override
        public long toLong(boolean upper) {
            return (long) value;
        }

        @Override
        public double toDouble(boolean upper) {
            return value;
        }

        @Override
        public boolean hasLong(boolean upper) {
            return upper ? (inclusive ? value >= Long.MIN_VALUE : value > Long.MIN_VALUE) : value < -(double) Long.MIN_VALUE;
        }

        @Override
        public long effectiveLong(boolean upper) {
            return upper
                    ? (inclusive ? (long) Math.floor(value) : (long) Math.ceil(value) - 1)
                    : (inclusive ? (long) Math.ceil(value) : (long) Math.floor(value) + 1);
        }

        @Override
        public double effectiveDouble(boolean upper) {
            return inclusive ? value : upper ? Math.nextDown(value) : Math.nextUp(value);
        }

        @Override
        public boolean matches(@NotNull Bound other) {
            return switch (other) {
                case OfDouble bound -> value == bound.value && inclusive == bound.inclusive;
                case OfLong bound -> NumberUtils.compare(bound.value, value) == 0 && inclusive == bound.inclusive;
                default -> false;
            };
        }

        @Override
        public @NotNull String toString() {
            return Double.toString(value);
        }
    }

    /**
     * An unbounded side, which admits every value but {@code NaN}.
     */
    enum Infinite implements Bound {
        /**
         * The only infinite bound.
         */
        INSTANCE;

        @Override
        public boolean inclusive() {
            return false;
        }

        @Override
        public boolean admits(long value, boolean upper) {
            return true;
        }

        @Override
        public boolean admits(double value, boolean upper) {
            return !Double.isNaN(value);
        }

        @Override
        public long toLong(boolean upper) {
            return upper ? Long.MAX_VALUE : Long.MIN_VALUE;
        }

        @Override
        public double toDouble(boolean upper) {
            return upper ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }

        @Override
        public boolean hasLong(boolean upper) {
            return true;
        }

        @Override
        public long effectiveLong(boolean upper) {
            return toLong(upper);
        }

        @Override
        public double effectiveDouble(boolean upper) {
            return toDouble(upper);
        }

        @Override
        public boolean matches(@NotNull Bound other) {
            return other == this;
        }

        @Override
        public @NotNull String toString() {
            return "";
        }
    }

    /**
     * A side that admits nothing, from a {@code NaN} value or an infinity on the opposite side of a {@link Range}.
     * Both sides of {@link Range#EMPTY} are invalid.
     */
    enum Invalid implements Bound {
        /**
         * The only invalid bound.
         */
        INSTANCE;

        @Override
        public boolean inclusive() {
            return false;
        }

        @Override
        public boolean admits(long value, boolean upper) {
            return false;
        }

        @Override
        public boolean admits(double value, boolean upper) {
            return false;
        }

        @Override
        public long toLong(boolean upper) {
            return upper ? Long.MIN_VALUE : Long.MAX_VALUE;
        }

        @Override
        public double toDouble(boolean upper) {
            return Double.NaN;
        }

        @Override
        public boolean hasLong(boolean upper) {
            return false;
        }

        @Override
        public long effectiveLong(boolean upper) {
            return toLong(upper);
        }

        @Override
        public double effectiveDouble(boolean upper) {
            return Double.NaN;
        }

        @Override
        public boolean matches(@NotNull Bound other) {
            return other == this;
        }

        @Override
        public @NotNull String toString() {
            return "NaN";
        }
    }
}
