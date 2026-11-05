package ink.glowing.utils.bool;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

/**
 * Helpers for working with boolean logic.
 */
public final class BooleanUtils {
    private BooleanUtils() { }

    /**
     * Returns a shared predicate that ignores its input and always gives the value.
     * @param <$Type> the type of the input
     * @param value the result of the predicate
     * @return the persistent predicate
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <$Type> @NotNull Predicate<$Type> always(boolean value) {
        return (Predicate<$Type>) Always.of(value);
    }

    /**
     * Returns a shared bi-predicate that ignores its inputs and always gives the value.
     * @param <$First> the type of the first input
     * @param <$Second> the type of the second input
     * @param value the result of the bi-predicate
     * @return the persistent bi-predicate
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <$First, $Second> @NotNull BiPredicate<$First, $Second> biAlways(boolean value) {
        return (BiPredicate<$First, $Second>) Always.of(value);
    }

    /**
     * Returns a shared {@code int} predicate that ignores its input and always gives the value.
     * @param value the result of the predicate
     * @return the persistent predicate
     */
    @Contract(pure = true)
    public static @NotNull IntPredicate intAlways(boolean value) {
        return Always.of(value);
    }

    /**
     * Returns a shared {@code long} predicate that ignores its input and always gives the value.
     * @param value the result of the predicate
     * @return the persistent predicate
     */
    @Contract(pure = true)
    public static @NotNull LongPredicate longAlways(boolean value) {
        return Always.of(value);
    }

    /**
     * Returns a shared {@code double} predicate that ignores its input and always gives the value.
     * @param value the result of the predicate
     * @return the persistent predicate
     */
    @Contract(pure = true)
    public static @NotNull DoublePredicate doubleAlways(boolean value) {
        return Always.of(value);
    }

    /**
     * Returns a shared supplier that always gives the value.
     * @param value the supplied value
     * @return the supplier
     */
    @Contract(pure = true)
    public static @NotNull BooleanSupplier alwaysSupply(boolean value) {
        return Always.of(value);
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

    /**
     * Parses {@code true} or {@code false}, ignoring case, rejecting anything else.
     * @param text the text to parse
     * @return the parsed value
     * @throws IllegalArgumentException if the text is neither {@code true} nor {@code false}
     */
    @Contract(pure = true)
    public static boolean parseBooleanStrict(@NotNull String text) {
        if (text.equalsIgnoreCase("true")) return true;
        if (text.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Invalid boolean: " + text);
    }

    private enum Always implements Predicate<Object>, BiPredicate<Object, Object>, IntPredicate, LongPredicate, DoublePredicate, BooleanSupplier {
        TRUE(true), FALSE(false);

        private final boolean value;

        Always(boolean value) {
            this.value = value;
        }

        static @NotNull Always of(boolean value) {
            return value ? TRUE : FALSE;
        }

        @Override
        public boolean test(Object obj) {
            return value;
        }

        @Override
        public boolean test(Object first, Object second) {
            return value;
        }

        @Override
        public boolean test(int value) {
            return this.value;
        }

        @Override
        public boolean test(long value) {
            return this.value;
        }

        @Override
        public boolean test(double value) {
            return this.value;
        }

        @Override
        public boolean getAsBoolean() {
            return value;
        }

        @Override
        public @NotNull Always negate() {
            return of(!value);
        }
    }
}
