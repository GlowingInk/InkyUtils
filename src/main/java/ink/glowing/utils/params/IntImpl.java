package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

final class IntImpl extends TypedImpl {
    private final int number;

    IntImpl(int number) {
        this(number, Integer.toString(number));
    }

    IntImpl(int number, @NotNull String text) {
        super(text);
        this.number = number;
    }

    @Override
    public @NotNull OptionalInt asInt() {
        return OptionalInt.of(number);
    }

    @Override
    public int asInt(int def) {
        return number;
    }

    @Override
    public int asInt(@NotNull IntSupplier def) {
        return number;
    }

    @Override
    public @NotNull OptionalLong asLong() {
        return OptionalLong.of(number);
    }

    @Override
    public long asLong(long def) {
        return number;
    }

    @Override
    public long asLong(@NotNull LongSupplier def) {
        return number;
    }

    @Override
    public @NotNull OptionalDouble asDouble() {
        return OptionalDouble.of(number);
    }

    @Override
    public double asDouble(double def) {
        return number;
    }

    @Override
    public double asDouble(@NotNull DoubleSupplier def) {
        return number;
    }
}
