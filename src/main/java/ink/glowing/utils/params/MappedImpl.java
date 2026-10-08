package ink.glowing.utils.params;

import ink.glowing.utils.hash.CaseInsensitive;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static ink.glowing.utils.params.ParameterHelper.*;

final class MappedImpl extends CompoundImpl {
    static final Parameter EMPTY = new MappedImpl(EMPTY_RAW, CaseInsensitive.newLinkedMap(0));

    final Object2ObjectLinkedOpenCustomHashMap<String, Parameter> internalValue;

    MappedImpl(@NotNull Object2ObjectLinkedOpenCustomHashMap<String, Parameter> internalValue) {
        this(SERIALIZED_RAW, internalValue);
    }

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
        putOrRemove(copy, key, value);
        return new MappedImpl(copy);
    }

    @Override
    public @NotNull @Unmodifiable Parameter with(@NotNull Map<String, Parameter> entries) {
        if (entries.isEmpty()) return this;

        var copy = internalValue.clone();
        for (Map.Entry<String, Parameter> entry : entries.entrySet()) {
            String key = entry.getKey();
            if (key != null) putOrRemove(copy, key, entry.getValue());
        }
        return new MappedImpl(copy);
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
        return internalValue.getOrDefault(key, MissingImpl.INSTANCE);
    }

    @Override
    public @NotNull @Unmodifiable Parameter get(int index) {
        return get(Integer.toString(index));
    }
}
