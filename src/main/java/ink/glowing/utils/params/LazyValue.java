package ink.glowing.utils.params;

import ink.glowing.utils.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The raw string of a parameter, lazily sliced out of the parsed input.
 */
final class LazyValue implements Function<Parameter, String> {
    private Supplier<String> valueGetter;

    LazyValue(char[] input, int start, int end) {
        this.valueGetter = () -> {
            String value = TextUtils.substring(input, start, end);
            valueGetter = () -> value;
            return value;
        };
    }

    @Override
    public @NotNull String apply(Parameter parameter) {
        return valueGetter.get();
    }
}
