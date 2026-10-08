package ink.glowing.utils.params;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Function;

import static ink.glowing.utils.params.ParameterHelper.*;

final class ListedImpl extends CompoundImpl {
    static final Parameter EMPTY = new ListedImpl(EMPTY_RAW, List.of());

    final List<Parameter> internalValue;
    private Set<String> indexes; // racy lazy cache, see CompoundImpl

    ListedImpl(@NotNull List<Parameter> internalValue) {
        this(SERIALIZED_RAW, internalValue);
    }

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
        return new ListedImpl(Arrays.asList(copy));
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
        return new ListedImpl(Arrays.asList(copy));
    }

    @Override
    public @NotNull @Unmodifiable Parameter withInsert(int index, @Nullable Parameter value) {
        if (index < 0 || isAbsent(value)) return this;

        int size = internalValue.size();
        Parameter[] copy = internalValue.toArray(new Parameter[Math.max(size, index) + 1]);
        if (index < size) {
            System.arraycopy(copy, index, copy, index + 1, size - index);
        } else {
            Arrays.fill(copy, size, index, MissingImpl.INSTANCE);
        }
        copy[index] = value;
        return new ListedImpl(Arrays.asList(copy));
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
        return new ListedImpl(Arrays.asList(copy));
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
