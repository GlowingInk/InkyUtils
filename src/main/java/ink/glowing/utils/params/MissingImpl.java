package ink.glowing.utils.params;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

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
