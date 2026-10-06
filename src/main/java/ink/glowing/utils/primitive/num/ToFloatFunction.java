package ink.glowing.utils.primitive.num;

import it.unimi.dsi.fastutil.objects.Object2FloatFunction;

/**
 * A function producing a {@code float}, the missing counterpart of the JDK's function primitives
 * @param <$Type> the type of the argument
 */
@FunctionalInterface
public interface ToFloatFunction<$Type> extends Object2FloatFunction<$Type> {
    /**
     * Applies this function to the argument.
     * @param value the argument
     * @return the result
     */
    float applyAsFloat($Type value);

    @SuppressWarnings("unchecked")
    default float getFloat(Object key) {
        return applyAsFloat(($Type) key);
    }
}
