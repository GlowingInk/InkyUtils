package ink.glowing.utils.params;

import ink.glowing.utils.params.ParameterImpl.LazyValue;
import ink.glowing.utils.params.ParameterImpl.ListedImpl;
import ink.glowing.utils.params.ParameterImpl.MappedImpl;
import ink.glowing.utils.params.ParameterImpl.PlainImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static ink.glowing.utils.params.ParameterImpl.GLOBAL_VALUE;

public sealed interface Parameter extends Parameterizable permits Parameter.Listed, Parameter.Mapped, Parameter.Plain {
    int count();

    @NotNull String value();

    @NotNull String asParameterValue(boolean global);

    @Nullable Parameter get(@Nullable String key);

    @Nullable Parameter get(int index);

    default <$Result> @Nullable $Result map(@Nullable String key, @NotNull Function<@Nullable Parameter, ? extends $Result> mapper) {
        return mapper.apply(get(key));
    }

    default <$Result> @Nullable $Result map(int index, @NotNull Function<@Nullable Parameter, ? extends $Result> mapper) {
        return mapper.apply(get(index));
    }

    @Override
    default @NotNull Parameter asParameter() {
        return this;
    }

    sealed interface Plain extends Parameter permits PlainImpl {
        static @NotNull Plain of(@Nullable String value) {
            return value == null || value.isEmpty()
                    ? PlainImpl.EMPTY
                    : new PlainImpl(value);
        }
    }

    sealed interface Listed extends Parameter permits ListedImpl {
        static @NotNull Listed of(@Nullable List<Parameter> value) {
            return value == null || value.isEmpty()
                    ? ListedImpl.EMPTY
                    : new ListedImpl(GLOBAL_VALUE, List.copyOf(value));
        }

        static @NotNull Parameter.Listed parse(@NotNull String inputStr) {
            if (inputStr.isEmpty()) return ListedImpl.EMPTY;
            char[] input = inputStr.toCharArray();
            return new ListedImpl(
                    new LazyValue(input, 0, input.length),
                    new ParserImpl(input).parseList(0)
            );
        }
    }

    sealed interface Mapped extends Parameter permits MappedImpl {
        static @NotNull Mapped of(@Nullable Map<String, Parameter> value) {
            return value == null || value.isEmpty()
                    ? MappedImpl.EMPTY
                    : new MappedImpl(GLOBAL_VALUE, Map.copyOf(value));
        }

        static @NotNull Parameter.Mapped parse(@NotNull String inputStr) {
            if (inputStr.isEmpty()) return MappedImpl.EMPTY;
            char[] input = inputStr.toCharArray();
            return new MappedImpl(
                    new LazyValue(input, 0, input.length),
                    new ParserImpl(input).parseMap(0)
            );
        }
    }

    static @NotNull String escapePlainValue(@NotNull String value) {
        String escaped = value.replace("\\", "\\\\").replace("'", "\\'");
        return value.indexOf(' ') != -1
                ? '\'' + escaped + '\''
                : escaped;
    }
}
