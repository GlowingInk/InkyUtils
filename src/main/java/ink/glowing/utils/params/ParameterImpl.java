package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

final class ParameterImpl {
    private ParameterImpl() { }

    private static final int NOT_AN_INDEX = Integer.MIN_VALUE;

    private static int parseIndex(@NotNull String key) {
        int length = key.length();
        if (length == 0) return NOT_AN_INDEX;
        for (int i = 0; i < length; i++) {
            char ch = key.charAt(i);
            boolean sign = i == 0 && length > 1 && (ch == '-' || ch == '+');
            if (!sign && Character.digit(ch, 10) < 0) return NOT_AN_INDEX;
        }
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException _) { // out of the int range
            return NOT_AN_INDEX;
        }
    }

    private static @Nullable Parameter getByIndexKey(@NotNull Parameter self, @Nullable String key) {
        if (key == null) return self;
        int index = parseIndex(key);
        return index == NOT_AN_INDEX ? null : self.get(index);
    }

    static final class PlainImpl implements Parameter.Plain {
        static final Parameter.Plain EMPTY = new PlainImpl("", EMPTY_VALUE);

        private final String value;
        private final Function<Parameter, String> rawCompute;
        private String escaped; // racy lazy cache, see CompoundImpl

        PlainImpl(@NotNull String value, @NotNull Function<Parameter, String> rawCompute) {
            this.value = value;
            this.rawCompute = rawCompute;
        }

        @Override
        public @NotNull String value() {
            return value;
        }

        @Override
        public @NotNull String raw() {
            return rawCompute.apply(this);
        }

        @Override
        public @NotNull String serialize(boolean topLevel) {
            String cached = escaped;
            if (cached == null) {
                escaped = cached = Parameter.escape(value);
            }
            return cached;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof PlainImpl other && value.equals(other.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "PlainImpl[value=" + value + "]";
        }

        @Override
        public int count() {
            return 1;
        }

        @Override
        public @Nullable Parameter get(@Nullable String key) {
            return getByIndexKey(this, key);
        }

        @Override
        public @Nullable Parameter get(int index) {
            return index == SELF_INDEX || index == 0 ? this : null;
        }
    }

    /**
     * Shared base of the parameters that hold nested parameters: their fields, equality and the
     * serialization skeleton, which only differs in the brackets and how the entries are appended.
     * <p>
     * The serialized forms are cached. The cache is intentionally racy: threads that miss it at
     * the same time may compute the value more than once, but they all compute the same string,
     * and {@code String} is safely published through a data race.
     */
    private abstract static class CompoundImpl<$Internal> {
        final Function<Parameter, String> valueCompute;
        final $Internal internalValue;

        private String mainValue;
        private String nestedValue;
        private String unescapedRaw;

        CompoundImpl(@NotNull Function<Parameter, String> valueCompute, @NotNull $Internal internalValue) {
            this.valueCompute = valueCompute;
            this.internalValue = internalValue;
        }

        abstract int count();

        abstract @NotNull String raw();

        abstract char open();

        abstract char close();

        /**
         * Appends every entry followed by a space.
         */
        abstract void appendEntries(@NotNull StringBuilder sb);

        public final @NotNull String value() {
            String cached = unescapedRaw;
            if (cached == null) {
                unescapedRaw = cached = Parameter.unescape(raw());
            }
            return cached;
        }

        public final @NotNull String serialize(boolean topLevel) {
            String cached = topLevel ? mainValue : nestedValue;
            if (cached == null) {
                cached = computeValue(topLevel);
                if (topLevel) {
                    mainValue = cached;
                } else {
                    nestedValue = cached;
                }
            }
            return cached;
        }

        private @NotNull String computeValue(boolean main) {
            if (count() == 0) {
                return main ? "" : "" + open() + close();
            }
            StringBuilder sb = new StringBuilder();
            if (!main) sb.append(open());
            appendEntries(sb);
            if (main) {
                sb.setLength(sb.length() - 1);
            } else {
                sb.setCharAt(sb.length() - 1, close());
            }
            return sb.toString();
        }

        @Override
        public final boolean equals(Object obj) {
            return obj == this || obj instanceof CompoundImpl<?> other && getClass() == other.getClass()
                    && valueCompute.equals(other.valueCompute) && internalValue.equals(other.internalValue);
        }

        @Override
        public final int hashCode() {
            return Objects.hash(valueCompute, internalValue);
        }

        @Override
        public final String toString() {
            return getClass().getSimpleName() + "[valueCompute=" + valueCompute + ", internalValue=" + internalValue + "]";
        }
    }

    static final class ListedImpl extends CompoundImpl<List<Parameter>> implements Parameter.Listed {
        static Listed EMPTY = new ListedImpl(EMPTY_VALUE, List.of());

        ListedImpl(@NotNull Function<Parameter, String> valueCompute, @NotNull List<Parameter> internalValue) {
            super(valueCompute, internalValue);
        }

        @Override
        char open() {
            return '[';
        }

        @Override
        char close() {
            return ']';
        }

        @Override
        void appendEntries(@NotNull StringBuilder sb) {
            for (Parameter entry : internalValue) {
                sb.append(entry.serialize(false)).append(' ');
            }
        }

        @Override
        public @NotNull String raw() {
            return valueCompute.apply(this);
        }

        @Override
        public int count() {
            return internalValue.size();
        }

        @Override
        public @Nullable Parameter get(@Nullable String key) {
            return getByIndexKey(this, key);
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

    static final class MappedImpl extends CompoundImpl<Map<String, Parameter>> implements Parameter.Mapped {
        static Mapped EMPTY = new MappedImpl(EMPTY_VALUE, Map.of());

        MappedImpl(@NotNull Function<Parameter, String> valueCompute, @NotNull Map<String, Parameter> internalValue) {
            super(valueCompute, internalValue);
        }

        @Override
        char open() {
            return '{';
        }

        @Override
        char close() {
            return '}';
        }

        @Override
        void appendEntries(@NotNull StringBuilder sb) {
            for (var entry : internalValue.entrySet()) {
                sb.append(Parameter.escape(entry.getKey()))
                        .append(':')
                        .append(entry.getValue().serialize(false))
                        .append(' ');
            }
        }

        @Override
        public @NotNull String raw() {
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

    static final Function<Parameter, String> GLOBAL_VALUE = param -> param.serialize(true);

    static final Function<Parameter, String> RAW_IS_VALUE = Parameter::value;

    /**
     * The raw string of a parameter, lazily sliced out of the parsed input.
     */
    static class LazyValue implements Function<Parameter, String> {
        private volatile String value;
        private char[] input;
        private final int start;
        private final int end;

        private final Lock lock = new ReentrantLock();

        LazyValue(char[] input, int start, int end) {
            this.input = input;
            this.start = start;
            this.end = end;
        }

        @Override
        public @NotNull String apply(Parameter parameter) {
            String result = value;
            if (result != null) return result;
            lock.lock();
            try {
                if (value != null) return value;
                value = new String(input, start, end - start);
                input = null; // so it'll get GC'ed eventually... hopefully
                return value;
            } finally {
                lock.unlock();
            }
        }
    }
}
