package ink.glowing.utils.rng.weight;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

/**
 * An immutable source of random elements, where each element is picked with a probability
 * proportional to its weight. Picking does not remove elements, so the same element can be
 * picked repeatedly.
 * <p>
 * Elements with a weight that is zero, negative or {@code NaN} are never picked. If no element
 * is left, the picker is empty: {@link #isEmpty()} returns {@code true} and {@link #next} throws.
 * {@code null} is a valid element as long as it has a positive weight.
 * @param <$Type> the type of elements
 */
public interface WeightedPicker<$Type> {
    /**
     * Returns an empty {@code WeightedPicker}. Always the same instance.
     * @param <$Type> the type of elements
     * @return an empty picker
     */
    @SuppressWarnings("unchecked")
    static <$Type> @NotNull WeightedPicker<$Type> of() {
        return (WeightedPicker<$Type>) Empty.INSTANCE;
    }

    /**
     * Returns a {@code WeightedPicker} that always picks the specified element.
     * @param <$Type> the type of elements
     * @param element the only element
     * @return a picker of the single element
     */
    static <$Type> @NotNull WeightedPicker<$Type> of($Type element) {
        return _ -> element;
    }

    /**
     * Returns a {@code WeightedPicker} of the keys of the specified map, weighted by their values.
     * @param <$Type> the type of elements
     * @param elements the map of elements to their weights
     * @return a picker of the map's keys
     */
    static <$Type> @NotNull WeightedPicker<$Type> ofMapped(@NotNull Map<$Type, Double> elements) {
        return ofCollection(elements.keySet(), (t, _) -> elements.getOrDefault(t, 0d));
    }

    /**
     * Returns a {@code WeightedPicker} of the elements of the specified collection, weighted by
     * {@link Weightable#weight()}. {@code null} elements are never picked.
     * @param <$WeightableType> the type of elements
     * @param collection the elements to pick from
     * @return a picker of the collection's elements
     */
    static <$WeightableType extends Weightable> @NotNull WeightedPicker<$WeightableType> ofCollection(@NotNull Collection<$WeightableType> collection) {
        return ofCollection(collection, (t, _) -> t == null ? 0 : t.weight());
    }

    /**
     * Returns a {@code WeightedPicker} of the elements of the specified collection, weighted by
     * the specified function.
     * @param <$Type> the type of elements
     * @param collection the elements to pick from
     * @param funct the function computing the weight of an element
     * @return a picker of the collection's elements
     */
    static <$Type> @NotNull WeightedPicker<$Type> ofCollection(@NotNull Collection<$Type> collection, @NotNull ToDoubleFunction<$Type> funct) {
        return ofCollection(collection, (t, _) -> funct.applyAsDouble(t));
    }

    /**
     * Returns a {@code WeightedPicker} of the elements of the specified collection, weighted by
     * the specified function, which also receives the position of each element.
     * @param <$Type> the type of elements
     * @param collection the elements to pick from
     * @param funct the function computing the weight of an element
     * @return a picker of the collection's elements
     */
    static <$Type> @NotNull WeightedPicker<$Type> ofCollection(@NotNull Collection<$Type> collection, @NotNull WeightFunction<$Type> funct) {
        return switch (collection.size()) {
            case 0 -> of();
            case 1 -> {
                $Type elem = collection.iterator().next();
                yield funct.apply(elem, 0) > 0
                        ? of(elem)
                        : of();
            }
            default -> AliasMethod.tryAlias(collection, funct);
        };
    }

    /**
     * Returns whether this picker has no elements to pick from.
     * @return {@code true} if {@link #next} would throw
     */
    default boolean isEmpty() {
        return false;
    }

    /**
     * Picks a random element, with probability proportional to its weight.
     * @param rng the source of randomness
     * @return the picked element
     * @throws NoSuchElementException if this picker is empty
     */
    $Type next(@NotNull RandomGenerator rng);

    /**
     * Returns an infinite stream of picked elements.
     * @param rng the source of randomness
     * @return a stream calling {@link #next} for each element
     * @throws NoSuchElementException when the stream is consumed, if this picker is empty
     */
    default @NotNull Stream<$Type> stream(@NotNull RandomGenerator rng) {
        return Stream.generate(() -> next(rng));
    }

    /**
     * Picker without elements, see {@link WeightedPicker#of()}
     */
    final class Empty implements WeightedPicker<Object> {
        public static final Empty INSTANCE = new Empty();

        private Empty() { }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Object next(@NotNull RandomGenerator rng) {
            throw new NoSuchElementException("Picker is empty");
        }
    }

    /**
     * Based off Keith Schwarz's (htiek@cs.stanford.edu) AliasMethod.java
     * <a href="http://www.keithschwarz.com/darts-dice-coins/">darts-dice-coins</a>
     * @param <$Type> the type of elements
     */
    final class AliasMethod<$Type> implements WeightedPicker<$Type> {
        private final List<$Type> elements;

        private final int[] alias;
        private final double[] probabilities;

        private static <$Type> @NotNull WeightedPicker<$Type> tryAlias(@NotNull Collection<$Type> collection, @NotNull WeightFunction<$Type> funct) {
            double[] rawProbabilities = new double[collection.size()];
            ArrayList<$Type> elements = new ArrayList<>(collection.size());

            double weightsSum = 0;
            int index = 0;
            for ($Type item : collection) {
                double weight = funct.apply(item, index++);
                if (!(weight > 0)) continue; // also skips NaN
                elements.add(item);
                rawProbabilities[elements.size() - 1] = weight;
                weightsSum += weight;
            }

            return switch (elements.size()) {
                case 0 -> of();
                case 1 -> of(elements.getFirst());
                default -> {
                    elements.trimToSize();
                    yield new AliasMethod<>(elements, rawProbabilities, weightsSum);
                }
            };
        }

        private AliasMethod(@NotNull List<$Type> elements, double[] rawProbabilities, double weightsSum) {
            int size = elements.size();

            this.elements = elements;
            this.probabilities = new double[size];
            this.alias = new int[size];

            double averageProbability = 1d / size;

            int[] small = new int[size]; int smallSize = 0;
            int[] large = new int[size]; int largeSize = 0;

            for (int i = 0; i < size; ++i) {
                if ((rawProbabilities[i] /= weightsSum) < averageProbability) {
                    small[smallSize++] = i;
                } else {
                    large[largeSize++] = i;
                }
            }

            while (smallSize != 0 && largeSize != 0) {
                int less = small[--smallSize];
                int more = large[--largeSize];

                this.probabilities[less] = rawProbabilities[less] * size;
                this.alias[less] = more;

                rawProbabilities[more] += rawProbabilities[less] - averageProbability;
                if (rawProbabilities[more] < averageProbability) {
                    small[smallSize++] = more;
                } else {
                    large[largeSize++] = more;
                }
            }

            while (smallSize != 0) this.probabilities[small[--smallSize]] = 1;
            while (largeSize != 0) this.probabilities[large[--largeSize]] = 1;
        }

        @Override
        public $Type next(@NotNull RandomGenerator rng) {
            int column = rng.nextInt(this.probabilities.length);
            boolean coinToss = rng.nextDouble() < this.probabilities[column];
            return this.elements.get(coinToss ? column : this.alias[column]);
        }
    }
}

