package ink.glowing.params;

import ink.glowing.params.ParameterImpl.ListedImpl;
import ink.glowing.params.ParameterImpl.MappedImpl;
import ink.glowing.params.ParameterImpl.PlainImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public sealed interface Parameter extends Parameterizable permits Parameter.Listed, Parameter.Mapped, Parameter.Plain {
    int count();

    @NotNull String value();

    default @NotNull Optional<Parameter> find(@Nullable String key) {
        return Optional.ofNullable(get(key));
    }

    default @NotNull Optional<Parameter> find(int index) {
        return Optional.ofNullable(get(index));
    }

    @Nullable Parameter get(@Nullable String key);

    @Nullable Parameter get(int index);

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

    sealed interface Mapped extends Parameter permits MappedImpl {
        static @NotNull Mapped of(@Nullable Map<String, Parameter> value) {
            return value == null || value.isEmpty()
                    ? MappedImpl.EMPTY
                    : new MappedImpl(parameter -> asParameterValue(parameter, true), Map.copyOf(value));
        }
    }

    sealed interface Listed extends Parameter permits ListedImpl {
        static @NotNull Listed of(@Nullable List<Parameter> value) {
            return value == null || value.isEmpty()
                    ? ListedImpl.EMPTY
                    : new ListedImpl(parameter -> asParameterValue(parameter, true), List.copyOf(value));
        }
    }

    static @NotNull String asParameterValue(@NotNull Parameter parameter, boolean global) {
        return switch (parameter) {
            case PlainImpl(String value) -> escapePlainValue(value);
            case MappedImpl mapped -> {
                if (mapped.count() == 0) {
                    yield global ? "" : "{}";
                }
                StringBuilder sb = new StringBuilder();
                if (!global) sb.append('{');
                for (var entry : mapped.internalValue().entrySet()) {
                    sb.append(escapePlainValue(entry.getKey()))
                            .append(':')
                            .append(asParameterValue(entry.getValue(), false));
                    sb.append(' ');
                }
                if (global) {
                    sb.setLength(sb.length() - 1);
                } else {
                    sb.setCharAt(sb.length() - 1, '}');
                }
                yield sb.toString();
            }
            case ListedImpl listed -> {
                if (listed.count() == 0) {
                    yield global ? "" : "[]";
                }
                StringBuilder sb = new StringBuilder();
                if (!global) sb.append('[');
                for (var entry : listed.internalValue()) {
                    sb.append(asParameterValue(entry, false));
                    sb.append(' ');
                }
                if (global) {
                    sb.setLength(sb.length() - 1);
                } else {
                    sb.setCharAt(sb.length() - 1, ']');
                }
                yield sb.toString();
            }
        };
    }

    static @NotNull String escapePlainValue(@NotNull String value) {
        String escaped = value.replace("\\", "\\\\").replace("'", "\\'");
        return value.indexOf(' ') != -1
                ? '\'' + escaped + '\''
                : escaped;
    }
}
