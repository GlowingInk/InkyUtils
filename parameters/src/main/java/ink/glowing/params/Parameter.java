package ink.glowing.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface Parameter<$Value> {
    @Nullable
    $Value value();

    enum Empty implements Parameter<Void> {
        INSTANCE;

        @Override
        public @Nullable Void value() {
            return null;
        }

        @Override
        public String toString() {
            return "";
        }
    }

    record OfList(List<Parameter<?>> value) implements Parameter<List<Parameter<?>>> {
        @Override
        public @NotNull String toString() {
            StringBuilder builder = new StringBuilder().append('[').append(' ');
            for (var param : value) {
                builder.append(param).append(' ');
            }
            return builder.append(']').toString();
        }
    }

    record OfMap(Map<String, Parameter<?>> value, boolean global) implements Parameter<Map<String, Parameter<?>>> {
        public OfMap(Map<String, Parameter<?>> value) {
            this(value, false);
        }

        @Override
        public @NotNull String toString() {
            StringBuilder builder = new StringBuilder();
            if (!global) {
                builder.append('{').append(' ');
            }
            for (var entry : value.entrySet()) {
                builder.append(entry.getKey()).append(':').append(entry.getValue()).append(' ');
            }
            if (!global) {
                builder.append('}');
            }
            return builder.toString();
        }
    }

    record OfString(String value) implements Parameter<String> {
        @Override
        public @NotNull String toString() {
            return "'" + value.replace("'", "\\'") + "'";
        }
    }
}
