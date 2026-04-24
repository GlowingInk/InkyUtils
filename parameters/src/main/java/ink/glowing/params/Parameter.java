package ink.glowing.params;

import ink.glowing.params.ParameterImpl.ListedImpl;
import ink.glowing.params.ParameterImpl.MappedImpl;
import ink.glowing.params.ParameterImpl.PlainImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface Parameter {
    @Nullable String rawValue();

    int count();

    boolean isEmpty();

    sealed interface Plain extends Parameter permits PlainImpl { }

    sealed interface Mapped extends Parameter permits MappedImpl { }

    sealed interface Listed extends Parameter permits ListedImpl { }

    static @NotNull String asParameterValue(Parameter parameter, boolean global) {
        return switch (parameter) {
            case PlainImpl(String value) -> escapePlainValue(value);
            case MappedImpl mapped -> {
                if (mapped.isEmpty()) {
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
                if (listed.isEmpty()) {
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
