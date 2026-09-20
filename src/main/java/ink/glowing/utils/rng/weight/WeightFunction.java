package ink.glowing.utils.rng.weight;

import org.jetbrains.annotations.Nullable;

/**
 * Computes the weight of an element, given its position in the source collection.
 * Elements with a weight that is zero, negative or {@code NaN} are never picked.
 * @param <$Type> the type of elements
 */
@FunctionalInterface
public interface WeightFunction<$Type> {
    /**
     * Returns the weight of an element.
     * @param t the element
     * @param index the position of the element in the source collection's iteration order
     * @return the weight of the element
     */
    double apply(@Nullable $Type t, int index);
}
