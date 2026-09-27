package ink.glowing.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helpers for chaining operations on a value without a temporary variable.
 */
public final class FluentUtils {
    private FluentUtils() { }

    /**
     * Runs the action on the value and returns the value itself.
     * @param <$Type> the type of the value
     * @param value the value to pass to the action and return
     * @param action the action to run
     * @return the same value
     */
    @Contract("_, _ -> param1")
    public static <$Type> $Type peek($Type value, @NotNull Consumer<? super $Type> action) {
        action.accept(value);
        return value;
    }

    /**
     * Applies the mapper to the value and returns the result.
     * @param <$Type> the type of the value
     * @param <$Result> the type of the result
     * @param value the value to map
     * @param mapper the function to apply
     * @return the result of the mapper
     */
    public static <$Type, $Result> $Result map($Type value, @NotNull Function<? super $Type, ? extends $Result> mapper) {
        return mapper.apply(value);
    }

    /**
     * Returns the value, or the fallback if the value is {@code null}.
     * @param <$Type> the type of the value
     * @param value the value to return if not {@code null}
     * @param def the fallback, may also be {@code null}
     * @return the value, or the fallback
     */
    @Contract(value = "!null, _ -> param1; null, _ -> param2", pure = true)
    public static <$Type> $Type orElse(@Nullable $Type value, @Nullable $Type def) {
        return value != null ? value : def;
    }

    /**
     * Returns the value, or the supplied fallback if the value is {@code null}.
     * The supplier is called only when needed.
     * @param <$Type> the type of the value
     * @param value the value to return if not {@code null}
     * @param def the supplier of the fallback
     * @return the value, or the supplied fallback
     */
    @Contract("!null, _ -> param1")
    public static <$Type> $Type orElseGet(@Nullable $Type value, @NotNull Supplier<? extends $Type> def) {
        return value != null ? value : def.get();
    }
}
