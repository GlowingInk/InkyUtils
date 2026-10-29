package ink.glowing.utils.num;

import ink.glowing.utils.num.Range.Interval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

final class RangeImpl {
    private RangeImpl() { }

    static @NotNull Interval interval(@NotNull Bound min, @NotNull Bound max) {
        IntervalImpl interval = new IntervalImpl(normalize(min, false), normalize(max, true));
        return interval.minDouble <= interval.maxDouble || interval.minLong <= interval.maxLong ? interval : Range.EMPTY;
    }

    private static @NotNull Bound normalize(@NotNull Bound bound, boolean upper) {
        return bound instanceof Bound.OfDouble(double value, boolean _) && Double.isInfinite(value)
                ? (value > 0 == upper ? Bound.Infinite.INSTANCE : Bound.Invalid.INSTANCE)
                : bound;
    }

    static @NotNull Range parse(@NotNull String str) {
        List<Interval> intervals = new ArrayList<>();
        int start = 0;
        while (true) {
            int end = str.indexOf('U', start);
            Interval interval = parseInterval(str.substring(start, end < 0 ? str.length() : end));
            if (interval == null) return Range.EMPTY;
            if (interval != Range.EMPTY) intervals.add(interval);
            if (end < 0) return union(intervals);
            start = end + 1;
        }
    }

    static @Nullable Interval parseInterval(@NotNull String str) {
        String interval = str.strip();
        int comma = interval.indexOf(',');
        if (comma < 0) return null;
        char open = interval.charAt(0);
        if (open != '[' && open != '(') return null;
        char close = interval.charAt(interval.length() - 1);
        if (close != ']' && close != ')') return null;
        Bound min = parseBound(interval.substring(1, comma), open == '[');
        Bound max = parseBound(interval.substring(comma + 1, interval.length() - 1), close == ']');
        if (min == null || max == null) return null;
        return interval(min, max);
    }

    private static @Nullable Bound parseBound(@NotNull String str, boolean inclusive) {
        String bound = str.strip();
        if (bound.isEmpty()) return Bound.infinite();
        Number value = NumberUtils.parseNumber(bound, null);
        if (value == null) return null;
        return inclusive ? Bound.inclusive(value) : Bound.exclusive(value);
    }

    static @NotNull Range or(@NotNull Range first, @NotNull Range second) {
        if (second.isEmpty()) return first;
        if (first.isEmpty()) return second;
        List<Interval> intervals = new ArrayList<>(intervals(first));
        intervals.addAll(intervals(second));
        return union(intervals);
    }

    private static @NotNull Range union(@NotNull List<Interval> intervals) {
        intervals.sort((left, right) -> compare(left.min(), false, right.min(), false));
        List<Interval> merged = new ArrayList<>(intervals.size());
        Interval current = null;
        for (Interval next : intervals) {
            if (current == null) {
                current = next;
            } else if (compare(next.min(), false, current.max(), true) > 0) {
                merged.add(current);
                current = next;
            } else if (compare(next.max(), true, current.max(), true) > 0) {
                current = new IntervalImpl(current.min(), next.max());
            }
        }
        if (current != null) merged.add(current);
        return compound(merged);
    }

    static @NotNull Range and(@NotNull Range first, @NotNull Range second) {
        if (first.isEmpty() || second.isEmpty()) return Range.EMPTY;
        List<Interval> left = intervals(first);
        List<Interval> right = intervals(second);
        List<Interval> common = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            Interval a = left.get(i);
            Interval b = right.get(j);
            boolean aEndsFirst = compare(a.max(), true, b.max(), true) <= 0;
            Interval overlap = interval(compare(a.min(), false, b.min(), false) >= 0 ? a.min() : b.min(), aEndsFirst ? a.max() : b.max());
            if (overlap != Range.EMPTY) common.add(overlap);
            if (aEndsFirst) i++;
            else j++;
        }
        return compound(common);
    }

    private static @NotNull List<Interval> intervals(@NotNull Range range) {
        return switch (range) {
            case Interval interval -> List.of(interval);
            case Range.Compound compound -> compound.intervals();
        };
    }

    private static @NotNull Range compound(@NotNull List<Interval> intervals) {
        return switch (intervals.size()) {
            case 0 -> Range.EMPTY;
            case 1 -> intervals.getFirst();
            default -> new CompoundImpl(List.copyOf(intervals));
        };
    }

    private static int compare(@NotNull Bound left, boolean leftUpper, @NotNull Bound right, boolean rightUpper) {
        if (left instanceof Bound.Infinite) return right instanceof Bound.Infinite && leftUpper == rightUpper ? 0 : leftUpper ? 1 : -1;
        if (right instanceof Bound.Infinite) return rightUpper ? -1 : 1;
        int comparison = switch (left) {
            case Bound.OfLong(long value, boolean _) -> switch (right) {
                case Bound.OfLong(long other, boolean _) -> Long.compare(value, other);
                case Bound.OfDouble(double other, boolean _) -> NumberUtils.compare(value, other);
                default -> 0;
            };
            case Bound.OfDouble(double value, boolean _) -> switch (right) {
                case Bound.OfLong(long other, boolean _) -> -NumberUtils.compare(other, value);
                // Not Double.compare - it puts -0.0 before 0.0
                case Bound.OfDouble(double other, boolean _) -> //noinspection UseCompareMethod
                        value < other ? -1 : value > other ? 1 : 0;
                default -> 0;
            };
            default -> 0;
        };
        return comparison != 0 ? comparison : Boolean.compare(left.inclusive() == leftUpper, right.inclusive() == rightUpper);
    }

    static final class IntervalImpl implements Interval {
        private final @NotNull Bound min;
        private final @NotNull Bound max;
        private final long minLong;
        private final long maxLong;
        private final double minDouble;
        private final double maxDouble;

        IntervalImpl(@NotNull Bound min, @NotNull Bound max) {
            this.min = min;
            this.max = max;
            boolean hasLong = min.hasLong(false) && max.hasLong(true);
            this.minLong = hasLong ? min.effectiveLong(false) : Long.MAX_VALUE;
            this.maxLong = hasLong ? max.effectiveLong(true) : Long.MIN_VALUE;
            this.minDouble = min.effectiveDouble(false);
            this.maxDouble = max.effectiveDouble(true);
        }

        @Override
        public @NotNull Bound min() {
            return min;
        }

        @Override
        public @NotNull Bound max() {
            return max;
        }

        @Override
        public boolean isEmpty() {
            return this == EMPTY;
        }

        @Override
        public boolean contains(long value) {
            return minLong <= value && value <= maxLong;
        }

        @Override
        public boolean contains(double value) {
            return minDouble <= value && value <= maxDouble;
        }

        @Override
        public boolean matches(@NotNull Range other) {
            return other instanceof Interval interval && min.matches(interval.min()) && max.matches(interval.max());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj == this || obj instanceof IntervalImpl other && min.equals(other.min) && max.equals(other.max);
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

    record CompoundImpl(@NotNull @Unmodifiable List<Interval> intervals) implements Range.Compound {
        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(long value) {
            for (Interval interval : intervals) {
                if (interval.contains(value)) return true;
            }
            return false;
        }

        @Override
        public boolean contains(double value) {
            for (Interval interval : intervals) {
                if (interval.contains(value)) return true;
            }
            return false;
        }

        @Override
        public boolean matches(@NotNull Range other) {
            if (!(other instanceof CompoundImpl(List<Interval> others)) || others.size() != intervals.size()) return false;
            for (int i = 0; i < intervals.size(); i++) {
                if (!intervals.get(i).matches(others.get(i))) return false;
            }
            return true;
        }

        @Override
        public @NotNull String toString() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < intervals.size(); i++) {
                if (i > 0) builder.append(" U ");
                builder.append(intervals.get(i));
            }
            return builder.toString();
        }
    }
}
