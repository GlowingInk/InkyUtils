package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

final class NumberValues {
    private NumberValues() { }

    static final class OfInt extends ValueBase.WithText {
        private final int number;

        OfInt(int number) {
            this(number, Integer.toString(number));
        }

        OfInt(int number, @NotNull String text) {
            super(text);
            this.number = number;
        }

        @Override
        public @NotNull OptionalInt asInt() {
            return OptionalInt.of(number);
        }

        @Override
        public int asInt(int fallback) {
            return number;
        }

        @Override
        public int asInt(@NotNull IntSupplier fallback) {
            return number;
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

    static final class OfLong extends ValueBase.WithText {
        private final long number;

        OfLong(long number) {
            this(number, Long.toString(number));
        }

        OfLong(long number, @NotNull String text) {
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

    static final class OfDouble extends ValueBase.WithText {
        private final double number;

        OfDouble(double number) {
            this(number, Double.toString(number));
        }

        OfDouble(double number, @NotNull String text) {
            super(text);
            this.number = number;
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
}
