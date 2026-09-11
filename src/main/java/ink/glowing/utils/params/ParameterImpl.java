package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

final class ParameterImpl {
    private ParameterImpl() { }

    record PlainImpl(@NotNull String value) implements Parameter.Plain {
        static final Parameter.Plain EMPTY = new PlainImpl("");

        @Override
        public @NotNull String asParameterValue(boolean main) {
            return Parameter.escapePlainValue(value);
        }

        @Override
        public int count() {
            return 1;
        }

        @Override
        public @Nullable Parameter get(@Nullable String key) {
            if (key == null) return this;
            try {
                return get(Integer.parseInt(key));
            } catch (NumberFormatException _) {
                return null;
            }
        }

        @Override
        public @Nullable Parameter get(int index) {
            return index == SELF_INDEX || index == 0 ? this : null;
        }
    }

    record ListedImpl(@NotNull Function<Parameter, String> valueCompute, @NotNull List<Parameter> internalValue) implements Parameter.Listed {
        static Listed EMPTY = new ListedImpl(EMPTY_VALUE, List.of());

        @Override
        public @NotNull String asParameterValue(boolean main) {
            if (count() == 0) {
                return !main ? "[]" : "";
            }
            StringBuilder sb = new StringBuilder();
            if (!main) sb.append('[');
            for (var entry : internalValue) {
                sb.append(entry.asParameterValue(false));
                sb.append(' ');
            }
            if (!main) {
                sb.setCharAt(sb.length() - 1, ']');
            } else {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }

        @Override
        public @NotNull String value() {
            return valueCompute.apply(this);
        }

        @Override
        public int count() {
            return internalValue.size();
        }

        @Override
        public @Nullable Parameter get(@Nullable String key) {
            if (key == null) {
                return this;
            }
            try {
                return get(Integer.parseInt(key));
            } catch (NumberFormatException _) {
                return null;
            }
        }

        @Override
        public @Nullable Parameter get(int index) {
            if (index == SELF_INDEX) {
                return this;
            } else if (index >= 0 && index < count()) {
                return internalValue.get(index);
            }
            return null;
        }
    }

    record MappedImpl(@NotNull Function<Parameter, String> valueCompute, @NotNull Map<String, Parameter> internalValue) implements Parameter.Mapped {
        static Mapped EMPTY = new MappedImpl(EMPTY_VALUE, Map.of());

        @Override
        public @NotNull String asParameterValue(boolean main) {
            if (count() == 0) {
                return !main ? "{}" : "";
            }
            StringBuilder sb = new StringBuilder();
            if (!main) sb.append('{');
            for (var entry : internalValue.entrySet()) {
                sb.append(Parameter.escapePlainValue(entry.getKey()))
                        .append(':')
                        .append(entry.getValue().asParameterValue(false));
                sb.append(' ');
            }
            if (!main) {
                sb.setCharAt(sb.length() - 1, '}');
            } else {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }

        @Override
        public @NotNull String value() {
            return valueCompute.apply(this);
        }

        @Override
        public int count() {
            return internalValue.size();
        }

        @Override
        public @Nullable Parameter get(@Nullable String key) {
            return key == null
                    ? this
                    : internalValue.get(key);
        }

        @Override
        public @Nullable Parameter get(int index) {
            return get(Integer.toString(index));
        }
    }

    static final Function<Parameter, String> EMPTY_VALUE = _ -> "";

    static final Function<Parameter, String> GLOBAL_VALUE = param -> param.asParameterValue(true);

    static class LazyValue implements Function<Parameter, String> {
        private volatile String value;
        private char[] input;
        private final int start;
        private final int inclusiveEnd;

        private final Lock lock = new ReentrantLock();

        LazyValue(char[] input, int start, int end) {
            this.input = input;
            this.start = start;
            this.inclusiveEnd = end - 1;
        }

        @Override
        public @NotNull String apply(Parameter parameter) {
            String result = value;
            if (result != null) return result;
            lock.lock();
            try {
                if (value != null) return value;
                StringBuilder builder = new StringBuilder(inclusiveEnd - start);
                for (int index = start; index <= inclusiveEnd; index++) {
                    char ch = input[index];
                    if (ch == '\\') {
                        builder.append(input[++index]); // guaranteed safe by the params parser
                        continue;
                    }
                    builder.append(ch);
                }
                value = builder.toString();
                input = null; // so it'll get GC'ed eventually... hopefully
                return value;
            } finally {
                lock.unlock();
            }
        }
    }
}
