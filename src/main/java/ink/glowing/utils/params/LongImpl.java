package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

final class LongImpl extends TypedImpl {
    private final long number;

    LongImpl(long number) {
        this(number, Long.toString(number));
    }

    LongImpl(long number, @NotNull String text) {
        super(text);
        this.number = number;
    }

    @Override
    public @NotNull OptionalLong asLong() {
        return OptionalLong.of(number);
    }

    @Override
    public long asLong(long fallback) {
        return number;
    }

    @Override
    public long asLong(@NotNull LongSupplier fallback) {
        return number;
    }

    @Override
    public @NotNull OptionalDouble asDouble() {
        return OptionalDouble.of(number);
    }

    @Override
    public double asDouble(double fallback) {
        return number;
    }

    @Override
    public double asDouble(@NotNull DoubleSupplier fallback) {
        return number;
    }
}
