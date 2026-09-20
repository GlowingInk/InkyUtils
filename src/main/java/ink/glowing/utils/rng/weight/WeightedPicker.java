package ink.glowing.utils.rng.weight;

import ink.glowing.utils.ComposerBase;
import ink.glowing.utils.rng.RngUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.jetbrains.annotations.Contract;
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
 * Elements with non-positive weights are never picked.
 * If any element has an infinite weight, all elements with a finite weight are discarded and
 * the infinite ones are picked uniformly.
 * If no element is left, the picker is empty: {@link #isEmpty()} returns {@code true} and
 * {@link #next} throws.
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
    static <$Type> @NotNull WeightedPicker<$Type> ofMapped(@NotNull Map<? extends $Type, @NotNull Double> elements) {
        return new Composer<$Type>(elements.size()).addAll(elements).finish();
    }

    /**
     * Returns a {@code WeightedPicker} of the keys of the specified fastutil map, weighted by
     * their primitive values.
     * @param <$Type> the type of elements
     * @param elements the map of elements to their weights
     * @return a picker of the map's keys
     */
    static <$Type> @NotNull WeightedPicker<$Type> ofMapped(@NotNull Object2DoubleMap<? extends $Type> elements) {
        return new Composer<$Type>(elements.size()).addAll(elements).finish();
    }

    /**
     * Returns a {@code WeightedPicker} of the elements of the specified collection, weighted by
     * {@link Weightable#weight()}. {@code null} elements are never picked.
     * @param <$WeightableType> the type of elements
     * @param collection the elements to pick from
     * @return a picker of the collection's elements
     */
    static <$WeightableType extends Weightable> @NotNull WeightedPicker<$WeightableType> ofCollection(@NotNull Collection<? extends $WeightableType> collection) {
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
    static <$Type> @NotNull WeightedPicker<$Type> ofCollection(@NotNull Collection<? extends $Type> collection, @NotNull ToDoubleFunction<? super $Type> funct) {
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
    static <$Type> @NotNull WeightedPicker<$Type> ofCollection(@NotNull Collection<? extends $Type> collection, @NotNull WeightFunction<? super $Type> funct) {
        return new Composer<$Type>(collection.size()).addAll(collection, funct).finish();
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

    /**
     * Picker where every element has the same probability.
     * @param <$Type> the type of elements
     */
    final class Uniform<$Type> implements WeightedPicker<$Type> {
        private final List<$Type> elements;

        private Uniform(@NotNull List<$Type> elements) {
            this.elements = elements;
        }

        @Override
        public $Type next(@NotNull RandomGenerator rng) {
            return RngUtils.randomElement(rng, this.elements);
        }
    }

    /**
     * A builder for constructing {@link WeightedPicker} instances.
     * <p>
     * Weights are checked while elements are added: an element with a weight that is not positive
     * (zero, negative or {@code NaN}) is dropped right away. Once an element with an infinite
     * weight is added, all elements with a finite weight are dropped, and the ones added later are
     * ignored. See {@link WeightedPicker} for details.
     * @param <$Type> the type of elements to be added to the picker
     */
    final class Composer<$Type> extends ComposerBase<WeightedPicker<$Type>> {
        private final ArrayList<$Type> elements;
        private final DoubleArrayList weights;
        private double weightsSum = 0;
        private boolean infinite = false;

        /**
         * Creates a new, empty {@code Composer}.
         */
        public Composer() {
            this.elements = new ArrayList<>();
            this.weights = new DoubleArrayList();
        }

        /**
         * Creates a new, empty {@code Composer}, sized for the expected number of elements.
         * @param expectedSize the expected number of added elements
         */
        public Composer(int expectedSize) {
            this.elements = new ArrayList<>(expectedSize);
            this.weights = new DoubleArrayList(expectedSize);
        }

        /**
         * Adds a single element with the specified weight to the picker being built.
         * @param element the element to add
         * @param weight the weight of the element
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_, _ -> this")
        public @NotNull Composer<$Type> add($Type element, double weight) {
            checkBuilt();
            _add(element, weight);
            return this;
        }

        private void _add($Type element, double weight) {
            if (weight == Double.POSITIVE_INFINITY) {
                if (!infinite) {
                    infinite = true;
                    elements.clear();
                    weights.clear();
                }
                elements.add(element);
            } else if (weight > 0 && !infinite) { // also skips NaN
                elements.add(element);
                weights.add(weight);
                weightsSum += weight;
            }
        }

        /**
         * Adds all keys of the specified map to the picker being built, weighted by their values.
         * @param elements the map of elements to their weights
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_ -> this")
        public @NotNull Composer<$Type> addAll(@NotNull Map<? extends $Type, @NotNull Double> elements) {
            checkBuilt();
            elements.forEach(this::_add);
            return this;
        }

        /**
         * Adds all keys of the specified fastutil map to the picker being built, weighted by
         * their primitive values.
         * @param elements the map of elements to their weights
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_ -> this")
        public @NotNull Composer<$Type> addAll(@NotNull Object2DoubleMap<? extends $Type> elements) {
            checkBuilt();
            elements.object2DoubleEntrySet().forEach(e -> _add(e.getKey(), e.getDoubleValue()));
            return this;
        }

        /**
         * Adds all elements of the specified iterable to the picker being built, weighted by
         * the specified function.
         * @param iterable the elements to add
         * @param funct the function computing the weight of an element
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_, _ -> this")
        public @NotNull Composer<$Type> addAll(@NotNull Iterable<? extends $Type> iterable, @NotNull ToDoubleFunction<? super $Type> funct) {
            return addAll(iterable, (t, _) -> funct.applyAsDouble(t));
        }

        /**
         * Adds all elements of the specified iterable to the picker being built, weighted by
         * the specified function, which also receives the position of each element in the iterable.
         * @param iterable the elements to add
         * @param funct the function computing the weight of an element
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_, _ -> this")
        public @NotNull Composer<$Type> addAll(@NotNull Iterable<? extends $Type> iterable, @NotNull WeightFunction<? super $Type> funct) {
            checkBuilt();
            int index = 0;
            for ($Type element : iterable) {
                _add(element, funct.apply(element, index++));
            }
            return this;
        }

        /**
         * Returns the number of elements that were added and not dropped so far.
         * @return the number of retained elements
         */
        public int size() {
            return elements.size();
        }

        /**
         * Returns whether no element was retained so far.
         * @return {@code true} if {@link #size()} is {@code 0}
         */
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        /**
         * Produces a {@code WeightedPicker} of all added elements that were not dropped.
         * @return a new picker, empty if no element was left
         */
        @Override
        protected @NotNull WeightedPicker<$Type> doFinish() {
            return switch (elements.size()) {
                case 0 -> of();
                case 1 -> of(elements.getFirst());
                default -> {
                    elements.trimToSize();
                    if (infinite) yield new Uniform<>(elements);

                    double[] rawProbabilities = weights.elements();
                    if (Double.isInfinite(weightsSum)) { // finite weights overflowed, rescale
                        double maxWeight = 0;
                        for (int i = 0; i < elements.size(); i++) {
                            maxWeight = Math.max(maxWeight, rawProbabilities[i]);
                        }
                        weightsSum = 0;
                        for (int i = 0; i < elements.size(); i++) {
                            weightsSum += rawProbabilities[i] /= maxWeight;
                        }
                    }
                    yield new AliasMethod<>(elements, rawProbabilities, weightsSum);
                }
            };
        }
    }
}
