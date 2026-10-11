package ink.glowing.utils.time;

import ink.glowing.utils.FluentUtils;
import ink.glowing.utils.hash.CaseInsensitive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Function;

/**
 * Utilities for {@link Duration}.
 */
public final class DurationUtils {
    private DurationUtils() { }

    private static final Duration MAX_DURATION = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);

    private static final Map<String, TemporalUnit> DEFAULT_UNITS_MAP = FluentUtils.map(Map.of(
            "ns", ChronoUnit.NANOS,
            "ms", ChronoUnit.MILLIS,
            "s", ChronoUnit.SECONDS,
            "m", ChronoUnit.MINUTES,
            "h", ChronoUnit.HOURS,
            "d", ChronoUnit.DAYS,
            "", ChronoUnit.SECONDS
    ), map -> Collections.unmodifiableMap(CaseInsensitive.newLinkedMap(map)));

    private static final Function<String, @Nullable TemporalUnit> DEFAULT_UNITS = DEFAULT_UNITS_MAP::get;

    /**
     * Returns the longest possible {@link Duration}: {@link Long#MAX_VALUE} seconds and
     * 999,999,999 nanoseconds. Same as the duration of {@link ChronoUnit#FOREVER}.
     * @return the maximal duration
     */
    @Contract(pure = true)
    public static @NotNull Duration maxDuration() {
        return MAX_DURATION;
    }

    /**
     * Returns the units used by {@link #parseDuration(String)}: {@code ns}, {@code ms}, {@code s},
     * {@code m}, {@code h} and {@code d}, and an empty suffix counts as seconds. Case-insensitive.
     * @return a function that resolves a suffix to its unit, or {@code null} if the suffix is unknown
     * @see #defaultUnitsMap()
     */
    @Contract(pure = true)
    public static @NotNull Function<String, @Nullable TemporalUnit> defaultUnits() {
        return DEFAULT_UNITS;
    }

    /**
     * Returns {@link #defaultUnits()} as a map, to pass to {@link #parseDuration(String, Map)} or to extend.
     * @return an immutable map of unit suffixes to units
     */
    @Contract(pure = true)
    public static @Unmodifiable @NotNull Map<String, TemporalUnit> defaultUnitsMap() {
        return DEFAULT_UNITS_MAP;
    }

    /**
     * Checks whether at least the required duration has passed between two points in time.
     * <p>
     * For example, {@code hasElapsed(lastUse, Instant.now(), cooldown)} tells whether a cooldown is over.
     * @param past the earlier point in time
     * @param now the later point in time
     * @param required the minimal duration that must pass
     * @return {@code true} if the duration between {@code past} and {@code now} is at least {@code required}
     * @throws java.time.DateTimeException if the duration between the temporals cannot be obtained
     * @throws ArithmeticException if the calculation overflows
     * @see Duration#between(Temporal, Temporal)
     */
    public static boolean hasElapsed(@NotNull Temporal past, @NotNull Temporal now, @NotNull Duration required) {
        return !Duration.between(past, now).minus(required).isNegative();
    }

    /**
     * Parses a duration using {@link #defaultUnits()}.
     * @param input the string to parse
     * @return the sum of all parts
     * @throws IllegalArgumentException if the input is invalid
     * @throws NumberFormatException if a part's number does not fit in a {@code long}
     * @throws ArithmeticException if the total duration overflows
     * @see #parseDuration(String, Function)
     * @see #defaultUnits()
     */
    @Contract(pure = true)
    public static @NotNull Duration parseDuration(@NotNull String input) {
        return parseDuration(input, DEFAULT_UNITS);
    }

    /**
     * Parses a duration using the given unit suffixes.
     * @param input the string to parse
     * @param units the unit suffixes; lookup follows the map's own key comparison
     * @return the sum of all parts
     * @throws IllegalArgumentException if the input is invalid
     * @throws NumberFormatException if a part's number does not fit in a {@code long}
     * @throws ArithmeticException if the total duration overflows
     * @see #parseDuration(String, Function)
     */
    @Contract(pure = true)
    public static @NotNull Duration parseDuration(@NotNull String input, @NotNull Map<String, ? extends TemporalUnit> units) {
        return parseDuration(input, units::get);
    }

    /**
     * Parses a duration from whitespace-separated parts, each an integer followed by
     * an optional unit suffix, and sums them. Blank input gives {@link Duration#ZERO}.
     * A part with a leading {@code -} is negative.
     * <p>
     * For example, with the default units {@code "1h 30m"} and {@code "90m"} parse to the same
     * duration, {@code "-1h -30m"} parses to its negation, and {@code "1h -30m"} parses to 30 minutes.
     * <p>
     * Units with an estimated duration, such as {@link ChronoUnit#MONTHS}, count as their
     * {@linkplain TemporalUnit#getDuration() estimated duration}, e.g. a month is 1/12 of 365.2425 days.
     * @param input the string to parse
     * @param units resolves a suffix to its unit, or {@code null} if the suffix is unknown
     * @return the sum of all parts
     * @throws IllegalArgumentException if a part is malformed or has an unknown suffix
     * @throws NumberFormatException if a part's number does not fit in a {@code long}
     * @throws ArithmeticException if the total duration overflows
     */
    public static @NotNull Duration parseDuration(@NotNull String input, @NotNull Function<String, ? extends @Nullable TemporalUnit> units) {
        if (input.isBlank()) return Duration.ZERO;

        StringTokenizer parts = new StringTokenizer(input);
        Duration result = Duration.ZERO;
        while (parts.hasMoreTokens()) {
            result = addPart(result, parts.nextToken(), units);
        }
        return result;
    }

    /**
     * Formats a duration as space-separated parts in days, hours, minutes, seconds, milliseconds and
     * nanoseconds, skipping zero parts, e.g. {@code "1h 30m"}. Negative durations get a {@code -} on every part,
     * e.g. {@code "-1h -30m"}. The result can be parsed back by {@link #parseDuration(String)}.
     * @param duration the duration to format
     * @return the formatted duration, {@code "0"} for {@link Duration#ZERO}, or {@code null} if the duration is {@code null}
     * @throws ArithmeticException if the duration is {@code Long.MIN_VALUE} seconds, which cannot be negated
     */
    @Contract(pure = true)
    public static @NotNull String serializeDuration(@NotNull Duration duration) {
        if (duration.isZero()) return "0";
        if (duration.isNegative()) return "-" + serializeDuration(duration.negated()).replace(" ", " -"); // otherwise -1ms would be "-1s 999ms"

        StringBuilder durationStr = new StringBuilder();

        appendTime(duration.toDaysPart(), "d", durationStr);
        appendTime(duration.toHoursPart(), "h", durationStr);
        appendTime(duration.toMinutesPart(), "m", durationStr);
        appendTime(duration.toSecondsPart(), "s", durationStr);
        appendTime(duration.toMillisPart(), "ms", durationStr);
        appendTime(duration.toNanosPart() % 1_000_000, "ns", durationStr);

        return durationStr.substring(0, durationStr.length() - 1);
    }

    private static void appendTime(long value, @NotNull String unit, @NotNull StringBuilder durationStr) {
        if (value != 0) durationStr.append(value).append(unit).append(' ');
    }

    private static @NotNull Duration addPart(
            @NotNull Duration result,
            @NotNull String part,
            @NotNull Function<String, ? extends @Nullable TemporalUnit> units
    ) {
        int digitsStart = part.startsWith("-") ? 1 : 0;
        int digitsEnd = digitsStart;
        while (digitsEnd < part.length() && Character.isDigit(part.charAt(digitsEnd))) {
            digitsEnd++;
        }
        if (digitsEnd == digitsStart) {
            throw new IllegalArgumentException("Invalid duration: " + part);
        }
        long value = Long.parseLong(part.substring(0, digitsEnd));
        String suffix = part.substring(digitsEnd);
        TemporalUnit unit = units.apply(suffix);
        if (unit == null) {
            throw new IllegalArgumentException("Invalid duration: " + part);
        }
        return unit.isDurationEstimated()
                ? result.plus(unit.getDuration().multipliedBy(value))
                : result.plus(value, unit);
    }
}
