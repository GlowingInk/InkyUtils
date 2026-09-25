package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

final class ParameterImpl {
    private ParameterImpl() { }

    private static @NotNull Parameter getByIndexKey(@NotNull Parameter self, @NotNull String key) {
        int length = key.length();
        if (length == 0) return Parameter.Missing.INSTANCE;
        for (int i = 0; i < length; i++) {
            char ch = key.charAt(i);
            boolean sign = i == 0 && length > 1 && (ch == '-' || ch == '+');
            if (!sign && Character.digit(ch, 10) < 0) return Parameter.Missing.INSTANCE;
        }
        try {
            return self.get(Integer.parseInt(key));
        } catch (NumberFormatException _) { // out of the int range
            return Parameter.Missing.INSTANCE;
        }
    }

    static final class ValueImpl implements Parameter.Value {
        static final Value EMPTY = new ValueImpl("", EMPTY_RAW);

        private final String value;
        private final Function<Parameter, String> rawCompute;
        private String escaped; // racy lazy cache, see CompoundImpl

        ValueImpl(@NotNull String value, @NotNull Function<Parameter, String> rawCompute) {
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
            String cached = escaped;
            if (cached == null) {
                escaped = cached = Parameter.escape(value);
            }
            return cached;
        }

        @Override
        public boolean matches(@NotNull Parameter other) {
            return other instanceof Value plain && value.equals(plain.textValue());
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof ValueImpl other && raw().equals(other.raw());
        }

        @Override
        public int hashCode() {
            return raw().hashCode();
        }

        @Override
        public String toString() {
            return textValue();
        }

        @Override
        public int count() {
            return 1;
        }

        @Override
        public @NotNull Parameter get(@NotNull String key) {
            return getByIndexKey(this, key);
        }

        @Override
        public @NotNull Parameter get(int index) {
            return index == 0 ? this : Parameter.Missing.INSTANCE;
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
    abstract static sealed class CompoundImpl implements Parameter {
        private final Function<Parameter, String> rawCompute;

        private String topLevelForm;
        private String nestedForm;
        private String unescapedRaw;

        CompoundImpl(@NotNull Function<Parameter, String> rawCompute) {
            this.rawCompute = rawCompute;
        }

        abstract char open();

        abstract char close();

        abstract @NotNull String empty();

        /**
         * Appends every entry followed by a space.
         */
        abstract void appendEntries(@NotNull StringBuilder sb);

        public final @NotNull String raw() {
            return rawCompute.apply(this);
        }

        public final @NotNull String textValue() {
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
                return topLevel ? "" : empty();
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
            return obj == this || obj != null && obj.getClass() == getClass() && raw().equals(((Parameter) obj).raw());
        }

        @Override
        public final int hashCode() {
            return raw().hashCode();
        }

        @Override
        public final String toString() {
            return textValue();
        }
    }

    static final class ListedImpl extends CompoundImpl implements Parameter.Listed {
        static final Listed EMPTY = new ListedImpl(EMPTY_RAW, List.of());

        private final List<Parameter> internalValue;

        ListedImpl(@NotNull Function<Parameter, String> rawCompute, @NotNull List<Parameter> internalValue) {
            super(rawCompute);
            this.internalValue = internalValue;
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
        @NotNull String empty() {
            return "[]";
        }

        @Override
        void appendEntries(@NotNull StringBuilder sb) {
            for (Parameter entry : internalValue) {
                sb.append(entry.serialize(false)).append(' ');
            }
        }

        @Override
        public boolean matches(@NotNull Parameter other) {
            if (!(other instanceof Parameter.Listed) || other.count() != count()) return false;
            for (int i = 0; i < internalValue.size(); i++) {
                if (!internalValue.get(i).matches(other.get(i))) return false;
            }
            return true;
        }

        @Override
        public int count() {
            return internalValue.size();
        }

        @Override
        public @NotNull Parameter get(@NotNull String key) {
            return getByIndexKey(this, key);
        }

        @Override
        public @NotNull Parameter get(int index) {
            if (index >= 0 && index < count()) {
                return internalValue.get(index);
            }
            return Parameter.Missing.INSTANCE;
        }
    }

    static final class MappedImpl extends CompoundImpl implements Parameter.Mapped {
        static final Mapped EMPTY = new MappedImpl(EMPTY_RAW, Map.of());

        private final Map<String, Parameter> internalValue;

        MappedImpl(@NotNull Function<Parameter, String> rawCompute, @NotNull Map<String, Parameter> internalValue) {
            super(rawCompute);
            this.internalValue = internalValue;
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
        @NotNull String empty() {
            return "{}";
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
        public boolean matches(@NotNull Parameter other) {
            if (!(other instanceof Parameter.Mapped) || other.count() != count()) return false;
            for (var entry : internalValue.entrySet()) {
                if (!entry.getValue().matches(other.get(entry.getKey()))) return false;
            }
            return true;
        }

        @Override
        public int count() {
            return internalValue.size();
        }

        @Override
        public @NotNull Parameter get(@Nullable String key) {
            return internalValue.getOrDefault(key, Parameter.Missing.INSTANCE);
        }

        @Override
        public @NotNull Parameter get(int index) {
            return get(Integer.toString(index));
        }
    }

    static final Function<Parameter, String> EMPTY_RAW = _ -> "";

    static final Function<Parameter, String> SERIALIZED_RAW = param -> param.serialize(true);

    static final Function<Parameter, String> VALUE_AS_RAW = Parameter::textValue;

    /**
     * The raw string of a parameter, lazily sliced out of the parsed input.
     */
    static class LazyValue implements Function<Parameter, String> {
        private Supplier<String> valueGetter;

        LazyValue(char[] input, int start, int end) {
            this.valueGetter = () -> {
                String value = new String(input, start, end - start);
                valueGetter = () -> value;
                return value;
            };
        }

        @Override
        public @NotNull String apply(Parameter parameter) {
            return valueGetter.get();
        }
    }
}
