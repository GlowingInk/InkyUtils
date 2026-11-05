package ink.glowing.utils.bool;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Helpers for working with boolean logic.
 */
public final class BooleanUtils {
    private static final Predicate<?> ALWAYS_TRUE = _ -> true;
    private static final Predicate<?> ALWAYS_FALSE = _ -> false;

    private BooleanUtils() { }

    /**
     * Returns a shared predicate that ignores its input and always gives the value.
     * @param <$Type> the type of the input
     * @param value the result of the predicate
     * @return {@link #alwaysTrue()} or {@link #alwaysFalse()}
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <$Type> @NotNull Predicate<$Type> always(boolean value) {
        return (Predicate<$Type>) (value ? ALWAYS_TRUE : ALWAYS_FALSE);
    }

    /**
     * Returns a shared predicate that accepts any input, including {@code null}.
     * @param <$Type> the type of the input
     * @return the predicate
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <$Type> @NotNull Predicate<$Type> alwaysTrue() {
        return (Predicate<$Type>) ALWAYS_TRUE;
    }

    /**
     * Returns a shared predicate that rejects any input, including {@code null}.
     * @param <$Type> the type of the input
     * @return the predicate
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <$Type> @NotNull Predicate<$Type> alwaysFalse() {
        return (Predicate<$Type>) ALWAYS_FALSE;
    }

    /**
     * Parses {@code true} or {@code false}, ignoring case.
     * @param text the text to parse
     * @return the parsed value, or {@code null} if the text is {@code null} or not recognized
     * @see TriState#parse(String)
     */
    @Contract(value = "null -> null", pure = true)
    public static @Nullable Boolean parseBoolean(@Nullable String text) {
        if ("true".equalsIgnoreCase(text)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(text)) return Boolean.FALSE;
        return null;
    }

    /**
     * Parses {@code true} or {@code false}, ignoring case.
     * @param text the text to parse
     * @param fallback the fallback, used if the text is {@code null} or not recognized
     * @return the parsed value, or the fallback
     */
    @Contract(value = "null, _ -> param2", pure = true)
    public static boolean parseBoolean(@Nullable String text, boolean fallback) {
        if ("true".equalsIgnoreCase(text)) return true;
        if ("false".equalsIgnoreCase(text)) return false;
        return fallback;
    }

    /**
     * Parses {@code true} or {@code false}, ignoring case, with a lazily computed fallback.
     * @param text the text to parse
     * @param fallback the supplier of the fallback, called only if the text is {@code null} or not recognized
     * @return the parsed value, or the fallback
     * @see #parseBoolean(String, boolean)
     */
    public static boolean parseBoolean(@Nullable String text, @NotNull BooleanSupplier fallback) {
        if ("true".equalsIgnoreCase(text)) return true;
        if ("false".equalsIgnoreCase(text)) return false;
        return fallback.getAsBoolean();
    }
}
