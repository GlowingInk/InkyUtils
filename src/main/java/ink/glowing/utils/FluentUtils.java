package ink.glowing.utils;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public final class FluentUtils {
    public static <$Type> $Type peek($Type value, @NotNull Consumer<$Type> action) {
        action.accept(value);
        return value;
    }

    public static <$Type, $Result> $Result map($Type value, @NotNull Function<$Type, $Result> mapper) {
        return mapper.apply(value);
    }
}
