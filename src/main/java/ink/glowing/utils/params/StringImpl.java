package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static ink.glowing.utils.params.ParameterHelper.EMPTY_RAW;
import static ink.glowing.utils.params.ParameterHelper.SERIALIZED_RAW;

final class StringImpl extends ValueImpl {
    static final Parameter EMPTY = new StringImpl("", EMPTY_RAW);

    private final String value;
    private final Function<Parameter, String> rawCompute;
    private String serialized; // racy lazy cache, see CompoundImpl

    StringImpl(@NotNull String value) {
        this(value, SERIALIZED_RAW);
    }

    StringImpl(@NotNull String value, @NotNull Function<Parameter, String> rawCompute) {
        this.value = value;
        this.rawCompute = rawCompute;
    }

    @Override
    public @NotNull String textValue() {
        return value;
    }

    @Override
    public @NotNull String raw() {
        return rawCompute.apply(this);
    }

    @Override
    public @NotNull String serialize(boolean topLevel) {
        String cached = serialized;
        if (cached == null) {
            serialized = cached = Parameter.escape(value);
        }
        return cached;
    }
}
