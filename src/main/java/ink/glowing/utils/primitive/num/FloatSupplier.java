package ink.glowing.utils.primitive.num;

/**
 * A supplier of {@code float} results, the missing counterpart of the JDK's supplier primitives.
 */
@FunctionalInterface
public interface FloatSupplier {
    /**
     * Gets a result.
     * @return the result
     */
    float getAsFloat();
}
