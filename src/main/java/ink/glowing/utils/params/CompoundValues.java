package ink.glowing.utils.params;

import ink.glowing.utils.hash.CaseInsensitive;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Function;

import static ink.glowing.utils.params.ParameterHelper.*;

/**
 * Shared base of the parameters that hold nested parameters: their fields, equality and the
 * serialization skeleton, which only differs in the brackets and how the entries are appended.
 * <p>
 * The serialized forms are cached. The cache is intentionally racy: threads that miss it at
 * the same time may compute the value more than once, but they all compute the same string,
 * and {@code String} is safely published through a data race.
 */
abstract sealed class CompoundValues implements Parameter permits CompoundValues.OfList, CompoundValues.OfMap {
    private final Function<Parameter, String> rawCompute;

    private String topLevelForm;
    private String nestedForm;
    private String unescapedRaw;

    CompoundValues(@NotNull Function<Parameter, String> rawCompute) {
        this.rawCompute = rawCompute;
    }

    abstract char open();

    abstract char close();

    /**
     * Appends every entry followed by a space.
     */
    abstract void appendEntries(@NotNull StringBuilder sb);

    @Override
    public final @NotNull String raw() {
        return rawCompute.apply(this);
    }

    @Override
    public final @NotNull String textValue() {
        String cached = unescapedRaw;
        if (cached == null) {
            unescapedRaw = cached = Parameter.unescape(raw());
        }
        return cached;
    }

    @Override
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

    static final class OfMap extends CompoundValues {
        static final Parameter EMPTY = new OfMap(EMPTY_RAW, CaseInsensitive.newLinkedMap(0));

        final Object2ObjectLinkedOpenCustomHashMap<String, Parameter> internalValue;

        OfMap(@NotNull Object2ObjectLinkedOpenCustomHashMap<String, Parameter> internalValue) {
            this(SERIALIZED_RAW, internalValue);
        }

        OfMap(@NotNull Function<Parameter, String> rawCompute, @NotNull Object2ObjectLinkedOpenCustomHashMap<String, Parameter> internalValue) {
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
        void appendEntries(@NotNull StringBuilder sb) {
            for (var entry : internalValue.entrySet()) {
                sb.append(Parameter.escape(entry.getKey()))
                        .append(':')
                        .append(entry.getValue().serialize(false))
                        .append(' ');
            }
        }

        @Override
        public @NotNull @Unmodifiable Parameter with(@NotNull String key, @Nullable Parameter value) {
            boolean absent = isAbsent(value);
            Parameter current = internalValue.get(key);
            if (absent ? current == null : current == value) return this;

            var copy = internalValue.clone();
            putOrRemove(copy, key, value);
            return new OfMap(copy);
        }

        @Override
        public @NotNull @Unmodifiable Parameter with(@NotNull Map<String, Parameter> entries) {
            if (entries.isEmpty()) return this;

            var copy = internalValue.clone();
            for (Map.Entry<String, Parameter> entry : entries.entrySet()) {
                String key = entry.getKey();
                if (key != null) putOrRemove(copy, key, entry.getValue());
            }
            return new OfMap(copy);
        }

        private static void putOrRemove(Map<String, Parameter> map, String key, @Nullable Parameter value) {
            if (isAbsent(value)) {
                map.remove(key);
            } else {
                map.put(key, value);
            }
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
            return internalValue.getOrDefault(key, MissingValue.INSTANCE);
        }

        @Override
        public @NotNull @Unmodifiable Parameter get(int index) {
            return get(Integer.toString(index));
        }
    }

    static final class OfList extends CompoundValues {
        static final Parameter EMPTY = new OfList(EMPTY_RAW, List.of());

        final List<Parameter> internalValue;
        private Set<String> indexes; // racy lazy cache, see CompoundImpl

        OfList(@NotNull List<Parameter> internalValue) {
            this(SERIALIZED_RAW, internalValue);
        }

        OfList(@NotNull Function<Parameter, String> rawCompute, @NotNull List<Parameter> internalValue) {
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
        void appendEntries(@NotNull StringBuilder sb) {
            for (Parameter entry : internalValue) {
                sb.append(entry.serialize(false)).append(' ');
            }
        }

        @Override
        public @NotNull @Unmodifiable Parameter with(int index, @Nullable Parameter value) {
            int size = internalValue.size();
            if (index < 0 || index >= size && isAbsent(value)) return this;
            if (index < size && internalValue.get(index) == orMissing(value)) return this;

            Parameter[] copy = copyWithFiller(Math.max(size, index + 1));
            copy[index] = orMissing(value);
            return new OfList(Arrays.asList(copy));
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
            return new OfList(Arrays.asList(copy));
        }

        @Override
        public @NotNull @Unmodifiable Parameter withInsert(int index, @Nullable Parameter value) {
            if (index < 0 || isAbsent(value)) return this;

            int size = internalValue.size();
            Parameter[] copy = internalValue.toArray(new Parameter[Math.max(size, index) + 1]);
            if (index < size) {
                System.arraycopy(copy, index, copy, index + 1, size - index);
            } else {
                Arrays.fill(copy, size, index, MissingValue.INSTANCE);
            }
            copy[index] = value;
            return new OfList(Arrays.asList(copy));
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
            int baseSize = Math.max(size, inserts.getLast().leftInt());
            Parameter[] copy = new Parameter[baseSize + inserts.size()];
            int out = 0;
            int next = 0;
            for (int i = 0; i <= baseSize; i++) {
                while (next < inserts.size() && inserts.get(next).leftInt() == i) {
                    copy[out++] = inserts.get(next++).right();
                }
                if (i < baseSize) copy[out++] = i < size ? internalValue.get(i) : MissingValue.INSTANCE;
            }
            return new OfList(Arrays.asList(copy));
        }

        private @NotNull Parameter[] copyWithFiller(int newSize) {
            int size = internalValue.size();
            Parameter[] copy = internalValue.toArray(new Parameter[newSize]);
            if (newSize > size) Arrays.fill(copy, size, newSize, MissingValue.INSTANCE);
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
            return MissingValue.INSTANCE;
        }
    }
}
