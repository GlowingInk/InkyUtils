package ink.glowing.utils;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

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
    public static <$Type> $Type peek($Type value, @NotNull Consumer<$Type> action) {
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
    public static <$Type, $Result> $Result map($Type value, @NotNull Function<$Type, $Result> mapper) {
        return mapper.apply(value);
    }
}
