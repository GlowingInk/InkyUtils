package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

/**
 * A plain value that also holds its typed form: the accessor of its own type returns it
 * without parsing, while {@link #textValue()} stays the text it was created from.
 * The text is also the raw and the serialized form, as it never needs escaping.
 */
abstract sealed class TypedImpl extends ValueImpl permits IntImpl, LongImpl, DoubleImpl, BooleanImpl, EnumImpl {
    private final String text;

    TypedImpl(@NotNull String text) {
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
