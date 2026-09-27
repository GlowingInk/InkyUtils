package ink.glowing.utils.time;

import ink.glowing.utils.FluentUtils;
import ink.glowing.utils.hash.CaseInsensitive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Returns the units used by {@link #parseDuration(String)}: {@code ns}, {@code ms}, {@code s},
     * {@code m}, {@code h} and {@code d} with missing unit mapped to seconds. Case-insensitive.
     * @return an immutable map of unit suffixes to units
     */
    @Contract(pure = true)
    public static @NotNull Map<String, TemporalUnit> defaultUnits() {
        return DEFAULT_UNITS;
    }

    /**
     * Parses a duration using {@link #defaultUnits()}.
     * @param input the string to parse
     * @return the sum of all parts
     * @throws IllegalArgumentException if the input is invalid
     * @see #parseDuration(String, Map)
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
     * @throws IllegalArgumentException if a part is malformed or has an unknown suffix
     * @throws java.time.temporal.UnsupportedTemporalTypeException if a matched unit has an
     * estimated duration, such as {@link ChronoUnit#WEEKS} or {@link ChronoUnit#MONTHS}
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
     * @param input the string to parse
     * @param units resolves a suffix to its unit, or {@code null} if the suffix is unknown.
     * @return the sum of all parts
     * @throws IllegalArgumentException if a part is malformed or has an unknown suffix
     * @throws java.time.temporal.UnsupportedTemporalTypeException if a matched unit has an
     * estimated duration, such as {@link ChronoUnit#WEEKS} or {@link ChronoUnit#MONTHS}
     */
    public static @NotNull Duration parseDuration(@NotNull String input, @NotNull Function<String, ? extends @Nullable TemporalUnit> units) {
        if (input.isBlank()) return Duration.ZERO;

        StringTokenizer parts = new StringTokenizer(input);
        Duration result = Duration.ZERO;
        while (parts.hasMoreTokens()) {
            result = result.plus(parsePart(parts.nextToken(), units));
        }
        return result;
    }

    private static @NotNull Duration parsePart(@NotNull String part, @NotNull Function<String, ? extends @Nullable TemporalUnit> units) {
        int digitsEnd = 0;
        while (digitsEnd < part.length() && isDigit(part.charAt(digitsEnd))) {
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
        return Duration.of(value, unit);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
