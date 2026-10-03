package ink.glowing.utils.time;

import ink.glowing.utils.FluentUtils;
import ink.glowing.utils.hash.CaseInsensitive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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

    private static final Map<String, TemporalUnit> DEFAULT_UNITS = FluentUtils.map(Map.of(
            "ns", ChronoUnit.NANOS,
            "ms", ChronoUnit.MILLIS,
            "s", ChronoUnit.SECONDS,
            "m", ChronoUnit.MINUTES,
            "h", ChronoUnit.HOURS,
            "d", ChronoUnit.DAYS,
            "", ChronoUnit.SECONDS
    ), map -> Collections.unmodifiableMap(CaseInsensitive.newLinkedMap(map)));

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
     * {@code m}, {@code h} and {@code d} with missing unit mapped to seconds. Case-insensitive.
     * @return an immutable map of unit suffixes to units
     */
    @Contract(pure = true)
    public static @Unmodifiable @NotNull Map<String, TemporalUnit> defaultUnits() {
        return DEFAULT_UNITS;
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
     * Parses a duration from whitespace-separated parts, each a non-negative integer followed by
     * an optional unit suffix, and sums them. Blank input gives {@link Duration#ZERO}.
     * <p>
     * For example, with the default units {@code "1h 30m"} and {@code "90m"} parse to the same
     * duration.
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

    private static @NotNull Duration addPart(
            @NotNull Duration result,
            @NotNull String part,
            @NotNull Function<String, ? extends @Nullable TemporalUnit> units
    ) {
        int digitsEnd = 0;
        while (digitsEnd < part.length() && Character.isDigit(part.charAt(digitsEnd))) {
            digitsEnd++;
        }
        if (digitsEnd == 0) {
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
