package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

import static ink.glowing.utils.params.ParameterHelper.getByIndexKey;

abstract sealed class ValueBase implements Parameter permits StringValue, ValueBase.WithText {
    private static final Set<String> SINGLE_KEY = Set.of("0");

    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public boolean matches(@NotNull Parameter other) {
        return other instanceof ValueBase plain && textValue().equals(plain.textValue());
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof ValueBase other && raw().equals(other.raw());
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
        return index == 0 ? this : MissingValue.INSTANCE;
    }

    /**
     * A plain value that also holds its typed form: the accessor of its own type returns it
     * without parsing, while {@link #textValue()} stays the text it was created from.
     * The text is also the raw and the serialized form, as it never needs escaping.
     */
    abstract sealed static class WithText extends ValueBase permits NumberValues.OfInt, NumberValues.OfLong, NumberValues.OfDouble, BooleanValue, EnumValue {
        private final String text;

        WithText(@NotNull String text) {
            this.text = text;
        }

        @Override
        public final @NotNull String textValue() {
            return text;
        }

        @Override
        public final @NotNull String raw() {
            return text;
        }

        @Override
        public final @NotNull String serialize(boolean topLevel) {
            return text;
        }
    }
}
