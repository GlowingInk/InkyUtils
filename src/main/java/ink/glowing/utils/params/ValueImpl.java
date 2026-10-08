package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

import static ink.glowing.utils.params.ParameterHelper.getByIndexKey;

abstract sealed class ValueImpl implements Parameter permits StringImpl, TypedImpl {
    private static final Set<String> SINGLE_KEY = Set.of("0");

    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public boolean matches(@NotNull Parameter other) {
        return other instanceof ValueImpl plain && textValue().equals(plain.textValue());
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
