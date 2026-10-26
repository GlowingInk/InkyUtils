package ink.glowing.utils.num;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Helpers for working with numbers.
 */
public final class NumberUtils {
    private NumberUtils() { }

    /**
     * Parses an {@code int}.
     * @param str the string to parse
     * @return the parsed value, or empty if the string is not a number
     * @see Integer#parseInt(String)
     */
    @Contract(pure = true)
    public static @NotNull OptionalInt parseInt(@Nullable String str) {
        if (str == null || str.isEmpty()) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseInt(str));
        } catch (NumberFormatException _) {
            return OptionalInt.empty();
        }
    }

    /**
     * Parses an {@code int}.
     * @param str the string to parse
     * @param fallback the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Integer#parseInt(String)
     */
    @Contract(value = "null, _ -> param2", pure = true)
    public static int parseInt(@Nullable String str, int fallback) {
        if (str == null || str.isEmpty()) return fallback;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    /**
     * Parses an {@code int}, with a lazily computed fallback.
     * @param str the string to parse
     * @param fallback the supplier of the fallback, called only if the string is not a number
     * @return the parsed value, or the fallback if the string is not a number
     * @see #parseInt(String, int)
     */
    public static int parseInt(@Nullable String str, @NotNull IntSupplier fallback) {
        if (str == null || str.isEmpty()) return fallback.getAsInt();
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException _) {
            return fallback.getAsInt();
        }
    }

    /**
     * Parses a {@code long}.
     * @param str the string to parse
     * @return the parsed value, or empty if the string is not a number
     * @see Long#parseLong(String)
     */
    @Contract(pure = true)
    public static @NotNull OptionalLong parseLong(@Nullable String str) {
        if (str == null || str.isEmpty()) return OptionalLong.empty();
        try {
            return OptionalLong.of(Long.parseLong(str));
        } catch (NumberFormatException _) {
            return OptionalLong.empty();
        }
    }

    /**
     * Parses a {@code long}.
     * @param str the string to parse
     * @param fallback the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Long#parseLong(String)
     */
    @Contract(value = "null, _ -> param2", pure = true)
    public static long parseLong(@Nullable String str, long fallback) {
        if (str == null || str.isEmpty()) return fallback;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    /**
     * Parses a {@code long}, with a lazily computed fallback.
     * @param str the string to parse
     * @param fallback the supplier of the fallback, called only if the string is not a number
     * @return the parsed value, or the fallback if the string is not a number
     * @see #parseLong(String, long)
     */
    public static long parseLong(@Nullable String str, @NotNull LongSupplier fallback) {
        if (str == null || str.isEmpty()) return fallback.getAsLong();
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException _) {
            return fallback.getAsLong();
        }
    }

    /**
     * Parses a {@code double}.
     * @param str the string to parse
     * @return the parsed value, or empty if the string is not a number
     * @see Double#parseDouble(String)
     */
    @Contract(pure = true)
    public static @NotNull OptionalDouble parseDouble(@Nullable String str) {
        if (str == null || str.isEmpty()) return OptionalDouble.empty();
        try {
            return OptionalDouble.of(Double.parseDouble(str));
        } catch (NumberFormatException _) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Parses a {@code double}.
     * @param str the string to parse
     * @param fallback the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Double#parseDouble(String)
     */
    @Contract(value = "null, _ -> param2", pure = true)
    public static double parseDouble(@Nullable String str, double fallback) {
        if (str == null || str.isEmpty()) return fallback;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    /**
     * Parses a {@code double}, with a lazily computed fallback.
     * @param str the string to parse
     * @param fallback the supplier of the fallback, called only if the string is not a number
     * @return the parsed value, or the fallback if the string is not a number
     * @see #parseDouble(String, double)
     */
    public static double parseDouble(@Nullable String str, @NotNull DoubleSupplier fallback) {
        if (str == null || str.isEmpty()) return fallback.getAsDouble();
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException _) {
            return fallback.getAsDouble();
        }
    }

    /**
     * Parses a number into the narrowest of {@code Integer}, {@code Long} and {@code Double} that holds it.
     * Only plain decimal numbers are accepted, like {@code -12}, {@code 6.5} or {@code 1e3}: no whitespace,
     * {@code NaN}, {@code Infinity}, hexadecimal or type suffixes.
     * @param str the string to parse
     * @return the parsed number, or empty if the string is not a number
     */
    @Contract(pure = true)
    public static @NotNull Optional<Number> parseNumber(@Nullable String str) {
        if (str == null) return Optional.empty();
        int length = str.length();
        int digitsFrom = length > 1 && (str.charAt(0) == '+' || str.charAt(0) == '-') ? 1 : 0;
        boolean integer = true;
        boolean hasDigit = false;
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (ch == '.' || ch == 'e' || ch == 'E' || ch == '+' || ch == '-') {
                if (i >= digitsFrom) integer = false;
            } else {
                return Optional.empty();
            }
        }
        if (!hasDigit) return Optional.empty();

        if (integer) {
            try {
                return Optional.of(Integer.parseInt(str));
            } catch (NumberFormatException _) { /* too large, try the next type */ }
            try {
                return Optional.of(Long.parseLong(str));
            } catch (NumberFormatException _) { /* too large, try the next type */ }
        }
        try {
            return Optional.of(Double.parseDouble(str));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    /**
     * Compares a {@code long} with a {@code double} exactly, without rounding the {@code long} to a {@code double}
     * as {@code a < b} does: {@code 2^53 + 1} is greater than {@code 2^53} as a {@code double}.
     * Zeros of both signs are equal to {@code 0}, and {@code NaN} is greater than any {@code long},
     * like in {@link Double#compare(double, double)}.
     * @param tested the {@code long} to compare
     * @param against the {@code double} to compare
     * @return a negative number if {@code tested} is less than {@code against}, zero if they are equal,
     * and a positive number if {@code tested} is greater
     */
    @Contract(pure = true)
    public static int compare(long tested, double against) {
        if (Double.isNaN(against) || against >= -(double) Long.MIN_VALUE) return -1;
        if (against < Long.MIN_VALUE) return 1;
        long whole = (long) against;
        return tested != whole ? Long.compare(tested, whole) : against > whole ? -1 : against < whole ? 1 : 0;
    }
}
