package ink.glowing.utils.primitive.num;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    // TODO There are also bytes... But I don't care for now

    /**
     * Parses an {@code int}.
     * @param str the string to parse, may be {@code null}
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
     * @param str the string to parse, may be {@code null}
     * @param def the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Integer#parseInt(String)
     */
    @Contract(value = "null, _ -> param2", pure = true)
    public static int parseInt(@Nullable String str, int def) {
        if (str == null || str.isEmpty()) return def;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException _) {
            return def;
        }
    }

    /**
     * Parses an {@code int}, with a lazily computed fallback.
     * @param str the string to parse, may be {@code null}
     * @param def the supplier of the fallback, called only if the string is not a number
     * @return the parsed value, or the fallback if the string is not a number
     * @see #parseInt(String, int)
     */
    public static int parseInt(@Nullable String str, @NotNull IntSupplier def) {
        if (str == null || str.isEmpty()) return def.getAsInt();
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException _) {
            return def.getAsInt();
        }
    }

    /**
     * Parses a {@code long}.
     * @param str the string to parse, may be {@code null}
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
     * @param str the string to parse, may be {@code null}
     * @param def the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Long#parseLong(String)
     */
    @Contract(value = "null, _ -> param2", pure = true)
    public static long parseLong(@Nullable String str, long def) {
        if (str == null || str.isEmpty()) return def;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException _) {
            return def;
        }
    }

    /**
     * Parses a {@code long}, with a lazily computed fallback.
     * @param str the string to parse, may be {@code null}
     * @param def the supplier of the fallback, called only if the string is not a number
     * @return the parsed value, or the fallback if the string is not a number
     * @see #parseLong(String, long)
     */
    public static long parseLong(@Nullable String str, @NotNull LongSupplier def) {
        if (str == null || str.isEmpty()) return def.getAsLong();
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException _) {
            return def.getAsLong();
        }
    }

    /**
     * Parses a {@code double}.
     * @param str the string to parse, may be {@code null}
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
     * @param str the string to parse, may be {@code null}
     * @param def the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Double#parseDouble(String)
     */
    @Contract(value = "null, _ -> param2", pure = true)
    public static double parseDouble(@Nullable String str, double def) {
        if (str == null || str.isEmpty()) return def;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException _) {
            return def;
        }
    }

    /**
     * Parses a {@code double}, with a lazily computed fallback.
     * @param str the string to parse, may be {@code null}
     * @param def the supplier of the fallback, called only if the string is not a number
     * @return the parsed value, or the fallback if the string is not a number
     * @see #parseDouble(String, double)
     */
    public static double parseDouble(@Nullable String str, @NotNull DoubleSupplier def) {
        if (str == null || str.isEmpty()) return def.getAsDouble();
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException _) {
            return def.getAsDouble();
        }
    }
}
