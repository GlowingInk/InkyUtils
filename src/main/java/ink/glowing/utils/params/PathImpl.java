package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

final class PathImpl {
    private PathImpl() { }

    /**
     * Returns the path to a key, an index one if the key is a canonical non-negative {@code int},
     * which is how a list is entered without parsing the key on each lookup.
     */
    static @NotNull Parameter.Path segment(@NotNull String key) {
        int length = key.length();
        if (length == 0 || length > 9 || length > 1 && key.charAt(0) == '0') return new Key(key); // 9 digits always fit
        int index = 0;
        for (int i = 0; i < length; i++) {
            char ch = key.charAt(i);
            if (ch < '0' || ch > '9') return new Key(key);
            index = index * 10 + (ch - '0');
        }
        return new Index(index);
    }

    record Key(@NotNull String key) implements Parameter.Path {
        @Override
        public @NotNull Parameter traverse(@NotNull Parameter from) {
            return from.get(key);
        }
    }

    record Index(int index) implements Parameter.Path {
        @Override
        public @NotNull Parameter traverse(@NotNull Parameter from) {
            return from.get(index);
        }
    }

    record Chain(@NotNull @Unmodifiable List<Parameter.Path> steps) implements Parameter.Path {
        Chain {
            steps = List.copyOf(steps);
        }

        @Override
        public @NotNull Parameter traverse(@NotNull Parameter from) {
            for (Parameter.Path step : steps) from = step.traverse(from);
            return from;
        }
    }
}
