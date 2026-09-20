package ink.glowing.utils.rng.weight;

/**
 * An value that knows its own weight, for use with {@link WeightedPicker#ofCollection(java.util.Collection)}.
 * Weights are relative: only their ratio to other weights matters.
 */
public interface Weightable {
    /**
     * Returns the weight of this value. Elements with a weight that is zero, negative or
     * {@code NaN} are never picked.
     * @return the weight
     */
    double weight();

    /**
     * A plain value paired with a weight.
     * @param <$Type> the type of the wrapped value
     * @param value the wrapped value
     * @param weight the weight of the value
     */
    record Pair<$Type>($Type value, double weight) implements Weightable { }
}
