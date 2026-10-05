package ink.glowing.utils.params;

import ink.glowing.utils.TextUtils;
import ink.glowing.utils.hash.CaseInsensitive;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

final class ParameterImpl {
    private ParameterImpl() { }

    private static @NotNull Parameter getByIndexKey(@NotNull Parameter self, @NotNull String key) {
        int length = key.length();
        if (length == 0) return MissingImpl.INSTANCE;
        for (int i = 0; i < length; i++) {
            char ch = key.charAt(i);
            boolean sign = i == 0 && length > 1 && (ch == '-' || ch == '+');
            if (!sign && Character.digit(ch, 10) < 0) return MissingImpl.INSTANCE;
        }
        try {
            return self.get(Integer.parseInt(key));
        } catch (NumberFormatException _) { // out of the int range
            return MissingImpl.INSTANCE;
        }
    }

    static boolean isAbsent(@Nullable Parameter parameter) {
        return parameter == null || parameter == MissingImpl.INSTANCE;
    }

    static @NotNull Parameter orMissing(@Nullable Parameter parameter) {
        return parameter == null ? MissingImpl.INSTANCE : parameter;
    }

    private static @NotNull String serializeEntry(@NotNull Parameter entry) {
        return entry == MissingImpl.INSTANCE ? "''" : entry.serialize(false);
    }

    private static final Set<String> SINGLE_KEY = Set.of("0");

    /**
     * The only parameter that stands for an absent one, see {@link Parameter#missing()}.
     */
    enum MissingImpl implements Parameter {
        INSTANCE;

        @Override
        public int count() {
            return 0;
        }

        @Override
        public @NotNull @Unmodifiable Set<String> keys() {
            return Set.of();
        }

        @Override
        public @NotNull String raw() {
            return "";
        }

        @Override
        public @NotNull String textValue() {
            return "";
        }

        @Override
        public boolean matches(@NotNull Parameter other) {
            return other == this;
        }

        @Override
        public @NotNull String serialize(boolean topLevel) {
            return "";
        }

        @Override
        public boolean isMissing() {
            return true;
        }

        @Override
        @Contract(value = "_ -> this", pure = true)
        public @NotNull @Unmodifiable MissingImpl get(@Nullable String key) {
            return this;
        }

        @Override
        @Contract(value = "_ -> this", pure = true)
        public @NotNull @Unmodifiable MissingImpl get(int index) {
            return this;
        }

        @Override
        @Contract(value = "-> this", pure = true)
        public @NotNull @Unmodifiable MissingImpl asParameter() {
            return this;
        }

        @Override
        public String toString() {
            return textValue();
        }
    }

    static final class ValueImpl implements Parameter {
        static final Parameter EMPTY = new ValueImpl("", EMPTY_RAW);

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
            return other instanceof ValueImpl plain && value.equals(plain.value);
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
        public @NotNull @Unmodifiable Set<String> keys() {
            return SINGLE_KEY;
        }

        @Override
        public @NotNull @Unmodifiable Parameter get(@NotNull String key) {
            return getByIndexKey(this, key);
        }

        @Override
        public @NotNull @Unmodifiable Parameter get(int index) {
            return index == 0 ? this : MissingImpl.INSTANCE;
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

    static final class ListedImpl extends CompoundImpl {
        static final Parameter EMPTY = new ListedImpl(EMPTY_RAW, List.of());

        final List<Parameter> internalValue;
        private Set<String> indexes; // racy lazy cache, see CompoundImpl

        ListedImpl(@NotNull Function<Parameter, String> rawCompute, @NotNull List<Parameter> internalValue) {
            super(rawCompute);
            this.internalValue = internalValue;
        }

        @Override
        public boolean isList() {
            return true;
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
                sb.append(serializeEntry(entry)).append(' ');
            }
        }

        @Override
        public @NotNull @Unmodifiable Parameter with(int index, @Nullable Parameter value) {
            int size = internalValue.size();
            if (index < 0 || index >= size && isAbsent(value)) return this;
            if (index < size && internalValue.get(index) == orMissing(value)) return this;

            Parameter[] copy = copyWithFiller(Math.max(size, index + 1));
            copy[index] = orMissing(value);
            return new ListedImpl(SERIALIZED_RAW, Arrays.asList(copy));
        }

        @Override
        public @NotNull @Unmodifiable Parameter with(@NotNull Collection<? extends IntObjectPair<Parameter>> pairs) {
            int newSize = internalValue.size();
            boolean changed = false;
            for (IntObjectPair<Parameter> pair : pairs) {
                if (pair == null || pair.leftInt() < 0) continue;

                int index = pair.leftInt();
                if (index >= newSize && isAbsent(pair.right())) continue;
                newSize = Math.max(newSize, index + 1);
                changed = true;
            }
            if (!changed) return this;

            Parameter[] copy = copyWithFiller(newSize);
            for (IntObjectPair<Parameter> pair : pairs) {
                // An absent pair skipped above is out of range, or lands on a filler, which is the same missing
                if (pair != null && pair.leftInt() >= 0 && pair.leftInt() < newSize) {
                    copy[pair.leftInt()] = orMissing(pair.right());
                }
            }
            return new ListedImpl(SERIALIZED_RAW, Arrays.asList(copy));
        }

        @Override
        public @NotNull @Unmodifiable Parameter withInsert(@NotNull Collection<? extends IntObjectPair<Parameter>> pairs) {
            ArrayList<IntObjectPair<Parameter>> inserts = new ArrayList<>(pairs.size());
            for (IntObjectPair<Parameter> pair : pairs) {
                if (pair != null && pair.leftInt() >= 0 && !isAbsent(pair.right())) inserts.add(pair);
            }
            if (inserts.isEmpty()) return this;
            inserts.sort(Comparator.comparingInt(IntObjectPair::leftInt)); // stable

            int size = internalValue.size();
            int baseSize = Math.max(size, inserts.getLast().leftInt() + 1);
            Parameter[] copy = new Parameter[baseSize + inserts.size()];
            int out = 0;
            int next = 0;
            for (int i = 0; i < baseSize; i++) {
                copy[out++] = i < size ? internalValue.get(i) : MissingImpl.INSTANCE;
                while (next < inserts.size() && inserts.get(next).leftInt() == i) {
                    copy[out++] = inserts.get(next++).right();
                }
            }
            return new ListedImpl(SERIALIZED_RAW, Arrays.asList(copy));
        }

        private @NotNull Parameter[] copyWithFiller(int newSize) {
            int size = internalValue.size();
            Parameter[] copy = internalValue.toArray(new Parameter[newSize]);
            if (newSize > size) Arrays.fill(copy, size, newSize, MissingImpl.INSTANCE);
            return copy;
        }

        @Override
        public @NotNull ParameterEditor.OfList editList() {
            return new ParameterEditorImpl.OfListImpl(this);
        }

        @Override
        public boolean matches(@NotNull Parameter other) {
            if (!other.isList() || other.count() != count()) return false;
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
        public @NotNull @Unmodifiable Set<String> keys() {
            Set<String> cached = indexes;
            if (cached == null) {
                int size = internalValue.size();
                Set<String> set = LinkedHashSet.newLinkedHashSet(size);
                for (int i = 0; i < size; i++) {
                    set.add(Integer.toString(i));
                }
                indexes = cached = Collections.unmodifiableSet(set);
            }
            return cached;
        }

        @Override
        public @NotNull @Unmodifiable Parameter get(@NotNull String key) {
            return getByIndexKey(this, key);
        }

        @Override
        public @NotNull @Unmodifiable Parameter get(int index) {
            if (index >= 0 && index < count()) {
                return internalValue.get(index);
            }
            return MissingImpl.INSTANCE;
        }
    }

    static final class MappedImpl extends CompoundImpl {
        static final Parameter EMPTY = new MappedImpl(EMPTY_RAW, CaseInsensitive.newLinkedMap(0));

        final Object2ObjectLinkedOpenCustomHashMap<String, Parameter> internalValue;

        MappedImpl(@NotNull Function<Parameter, String> rawCompute, @NotNull Object2ObjectLinkedOpenCustomHashMap<String, Parameter> internalValue) {
            super(rawCompute);
            this.internalValue = internalValue;
        }

        @Override
        public boolean isMap() {
            return true;
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
                        .append(serializeEntry(entry.getValue()))
                        .append(' ');
            }
        }

        @Override
        public @NotNull @Unmodifiable Parameter with(@NotNull String key, @Nullable Parameter value) {
            boolean absent = isAbsent(value);
            Parameter current = internalValue.get(key);
            if (absent ? current == null : current == value) return this;

            var copy = internalValue.clone();
            if (absent) {
                copy.remove(key);
            } else {
                copy.put(key, value);
            }
            return new MappedImpl(SERIALIZED_RAW, copy);
        }

        @Override
        public @NotNull @Unmodifiable Parameter with(@NotNull Map<String, Parameter> entries) {
            if (entries.isEmpty()) return this;

            var copy = internalValue.clone();
            for (Map.Entry<String, Parameter> entry : entries.entrySet()) {
                String key = entry.getKey();
                if (key == null) continue;
                if (isAbsent(entry.getValue())) {
                    copy.remove(key);
                } else {
                    copy.put(key, entry.getValue());
                }
            }
            return new MappedImpl(SERIALIZED_RAW, copy);
        }

        @Override
        public @NotNull ParameterEditor.OfMap editMap() {
            return new ParameterEditorImpl.OfMapImpl(this);
        }

        @Override
        public boolean matches(@NotNull Parameter other) {
            if (!other.isMap() || other.count() != count()) return false;
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
        public @NotNull @Unmodifiable Set<String> keys() {
            return Collections.unmodifiableSet(internalValue.keySet());
        }

        @Override
        public @NotNull @Unmodifiable Parameter get(@Nullable String key) {
            return internalValue.getOrDefault(key, MissingImpl.INSTANCE);
        }

        @Override
        public @NotNull @Unmodifiable Parameter get(int index) {
            return get(Integer.toString(index));
        }
    }

    static final Function<Parameter, String> EMPTY_RAW = _ -> "";

    static final Function<Parameter, String> SERIALIZED_RAW = param -> param.serialize(true);

    static final Function<Parameter, String> VALUE_RAW = Parameter::textValue;

    /**
     * The raw string of a parameter, lazily sliced out of the parsed input.
     */
    static class LazyValue implements Function<Parameter, String> {
        private Supplier<String> valueGetter;

        LazyValue(char[] input, int start, int end) {
            this.valueGetter = () -> {
                String value = TextUtils.substring(input, start, end);
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
