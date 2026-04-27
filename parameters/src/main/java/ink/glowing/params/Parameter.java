package ink.glowing.params;

import ink.glowing.params.ParameterImpl.ListedImpl;
import ink.glowing.params.ParameterImpl.MappedImpl;
import ink.glowing.params.ParameterImpl.PlainImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public sealed interface Parameter permits Parameter.Listed, Parameter.Mapped, Parameter.Plain {
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

    sealed interface Plain extends Parameter permits PlainImpl { }

    sealed interface Mapped extends Parameter permits MappedImpl { }

    sealed interface Listed extends Parameter permits ListedImpl { }

    static @NotNull String asParameterValue(Parameter parameter, boolean global) {
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
