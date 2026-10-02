package ink.glowing.utils.primitive;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers for working with numbers.
 */
public final class NumberUtils {
    private NumberUtils() { }

    /**
     * Parses an {@code int}.
     * @param str the string to parse, may be {@code null}
     * @param def the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Integer#parseInt(String)
     */
    @Contract(pure = true)
    public static int parseInt(@Nullable String str, int def) {
        if (str == null || str.isEmpty()) return def;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException _) {
            return def;
        }
    }

    /**
     * Parses a {@code long}.
     * @param str the string to parse, may be {@code null}
     * @param def the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Long#parseLong(String)
     */
    @Contract(pure = true)
    public static long parseLong(@Nullable String str, long def) {
        if (str == null || str.isEmpty()) return def;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException _) {
            return def;
        }
    }

    /**
     * Parses a {@code float}.
     * @param str the string to parse, may be {@code null}
     * @param def the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Float#parseFloat(String)
     */
    @Contract(pure = true)
    public static float parseFloat(@Nullable String str, float def) {
        if (str == null || str.isEmpty()) return def;
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException _) {
            return def;
        }
    }

    /**
     * Parses a {@code double}.
     * @param str the string to parse, may be {@code null}
     * @param def the fallback
     * @return the parsed value, or the fallback if the string is not a number
     * @see Double#parseDouble(String)
     */
    @Contract(pure = true)
    public static double parseDouble(@Nullable String str, double def) {
        if (str == null || str.isEmpty()) return def;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException _) {
            return def;
        }
    }
}
