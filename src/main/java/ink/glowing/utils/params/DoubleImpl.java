package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;
import java.util.function.DoubleSupplier;

final class DoubleImpl extends TypedImpl {
    private final double number;

    DoubleImpl(double number) {
        this(number, Double.toString(number));
    }

    DoubleImpl(double number, @NotNull String text) {
        super(text);
        this.number = number;
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
