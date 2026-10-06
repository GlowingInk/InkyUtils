package ink.glowing.utils.primitive.num;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * A container that may hold a {@code float}, the missing counterpart of the JDK's
 * {@link java.util.OptionalInt}, {@link java.util.OptionalLong} and {@link java.util.OptionalDouble}.
 */
public sealed interface OptionalFloat permits OptionalFloat.Empty, OptionalFloat.Present {
    /**
     * Returns the empty {@code OptionalFloat}.
     * @return the empty instance
     */
    @Contract(pure = true)
    static @NotNull OptionalFloat empty() {
        return Empty.INSTANCE;
    }

    /**
     * Returns an {@code OptionalFloat} holding the value.
     * @param value the value to hold
     * @return the resulting instance
     */
    @Contract(pure = true)
    static @NotNull OptionalFloat of(float value) {
        return new Present(value);
    }

    /**
     * Returns an {@code OptionalFloat} holding the value, or the empty one if it is {@code null}.
     * @param value the value to hold, may be {@code null}
     * @return the resulting instance
     */
    @Contract(pure = true)
    static @NotNull OptionalFloat ofNullable(@Nullable Float value) {
        return value == null ? Empty.INSTANCE : new Present(value);
    }

    /**
     * Returns whether a value is present.
     * @return {@code true} if there is a value
     */
    @Contract(pure = true)
    boolean isPresent();

    /**
     * Returns whether no value is present.
     * @return {@code true} if there is no value
     */
    @Contract(pure = true)
    default boolean isEmpty() {
        return !isPresent();
    }

    /**
     * Returns the value.
     * @return the held value
     * @throws NoSuchElementException if there is no value
     */
    @Contract(pure = true)
    float getAsFloat();

    /**
     * Returns the value, or the fallback if there is none.
     * @param other the fallback
     * @return the held value or the fallback
     */
    @Contract(pure = true)
    float orElse(float other);

    /**
     * Returns the value, or throws the supplied exception if there is none.
     * @param <$Exception> the type of the exception
     * @param exceptionSupplier the supplier of the exception
     * @return the held value
     * @throws $Exception if there is no value
     */
    <$Exception extends Throwable> float orElseThrow(@NotNull Supplier<? extends $Exception> exceptionSupplier) throws $Exception;

    /**
     * The {@link OptionalFloat} holding nothing.
     */
    enum Empty implements OptionalFloat {
        /**
         * The only empty {@code OptionalFloat}.
         */
        INSTANCE;

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public float getAsFloat() {
            throw new NoSuchElementException("No value present");
        }

        @Override
        public float orElse(float other) {
            return other;
        }

        @Override
        public <$Exception extends Throwable> float orElseThrow(@NotNull Supplier<? extends $Exception> exceptionSupplier) throws $Exception {
            throw exceptionSupplier.get();
        }

        @Override
        public @NotNull String toString() {
            return "OptionalFloat.empty";
        }
    }

    /**
     * The {@link OptionalFloat} holding a value.
     * @param value the held value
     */
    record Present(float value) implements OptionalFloat {
        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public float getAsFloat() {
            return value;
        }

        @Override
        public float orElse(float other) {
            return value;
        }

        @Override
        public <$Exception extends Throwable> float orElseThrow(@NotNull Supplier<? extends $Exception> exceptionSupplier) {
            return value;
        }

        @Override
        public @NotNull String toString() {
            return "OptionalFloat[" + value + "]";
        }
    }
}
