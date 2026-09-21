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
        static final Parameter.Plain EMPTY = new PlainImpl("", EMPTY_RAW);

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
        public boolean matches(@NotNull Parameter other) {
            return other instanceof Parameter.Plain plain && value.equals(plain.value());
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof PlainImpl other && raw().equals(other.raw());
        }

        @Override
        public int hashCode() {
            return raw().hashCode();
        }

        @Override
        public String toString() {
            return "PlainImpl[raw=" + raw() + "]";
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
        final Function<Parameter, String> rawCompute;
        final $Internal internalValue;

        private String topLevelForm;
        private String nestedForm;
        private String unescapedRaw;

        CompoundImpl(@NotNull Function<Parameter, String> rawCompute, @NotNull $Internal internalValue) {
            this.rawCompute = rawCompute;
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
            String cached = topLevel ? topLevelForm : nestedForm;
            if (cached == null) {
                cached = computeSerialized(topLevel);
                if (topLevel) {
                    topLevelForm = cached;
                } else {
                    nestedForm = cached;
                }
            }
            return cached;
        }

        private @NotNull String computeSerialized(boolean topLevel) {
            if (count() == 0) {
                return topLevel ? "" : "" + open() + close();
            }
            StringBuilder sb = new StringBuilder();
            if (!topLevel) sb.append(open());
            appendEntries(sb);
            if (topLevel) {
                sb.setLength(sb.length() - 1);
            } else {
                sb.setCharAt(sb.length() - 1, close());
            }
            return sb.toString();
        }

        @Override
        public final boolean equals(Object obj) {
            return obj == this || obj instanceof CompoundImpl<?> other && getClass() == other.getClass() && raw().equals(other.raw());
        }

        @Override
        public final int hashCode() {
            return raw().hashCode();
        }

        @Override
        public final String toString() {
            return getClass().getSimpleName() + "[raw=" + raw() + "]";
        }
    }

    static final class ListedImpl extends CompoundImpl<List<Parameter>> implements Parameter.Listed {
        static Listed EMPTY = new ListedImpl(EMPTY_RAW, List.of());

        ListedImpl(@NotNull Function<Parameter, String> rawCompute, @NotNull List<Parameter> internalValue) {
            super(rawCompute, internalValue);
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
            return rawCompute.apply(this);
        }

        @Override
        public boolean matches(@NotNull Parameter other) {
            if (!(other instanceof Parameter.Listed) || other.count() != count()) return false;
            for (int i = 0; i < internalValue.size(); i++) {
                Parameter otherEntry = other.get(i);
                if (otherEntry == null || !internalValue.get(i).matches(otherEntry)) return false;
            }
            return true;
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
        static Mapped EMPTY = new MappedImpl(EMPTY_RAW, Map.of());

        MappedImpl(@NotNull Function<Parameter, String> rawCompute, @NotNull Map<String, Parameter> internalValue) {
            super(rawCompute, internalValue);
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
            return rawCompute.apply(this);
        }

        @Override
        public boolean matches(@NotNull Parameter other) {
            if (!(other instanceof Parameter.Mapped) || other.count() != count()) return false;
            for (var entry : internalValue.entrySet()) {
                Parameter otherEntry = other.get(entry.getKey());
                if (otherEntry == null || !entry.getValue().matches(otherEntry)) return false;
            }
            return true;
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

    static final Function<Parameter, String> EMPTY_RAW = _ -> "";

    static final Function<Parameter, String> SERIALIZED_RAW = param -> param.serialize(true);

    static final Function<Parameter, String> VALUE_AS_RAW = Parameter::value;

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
