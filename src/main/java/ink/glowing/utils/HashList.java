package ink.glowing.utils;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Array;
import java.util.*;

/**
 * An unmodifiable, hash-backed {@link List} implementation that provides O(1) performance
 * for {@link #contains(Object)}, {@link #indexOf(Object)}, and {@link #lastIndexOf(Object)}
 * operations, while maintaining indexed access via {@link RandomAccess}.
 * <p>
 * When a list is built with a custom {@link Hash.Strategy}, that same
 * strategy governs {@code contains}/{@code indexOf}/{@code lastIndexOf} <em>and</em>
 * {@link #equals(Object)}/{@link #hashCode()}, so element comparison is consistent across all
 * of them.
 * @param <$Type> the type of elements in this list
 */
@SuppressWarnings("unchecked")
@Unmodifiable
public sealed abstract class HashList<$Type> extends AbstractList<$Type> implements RandomAccess {
    /**
     * The default {@link Hash.Strategy}, used when no custom strategy is supplied.
     * Delegates to {@link Objects#hashCode(Object)} and {@link Objects#equals(Object, Object)},
     * matching standard {@link Object#equals}/{@link Object#hashCode} semantics (including
     * {@code null} elements).
     */
    public static final Hash.Strategy<?> STANDARD_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(Object o) {
            return Objects.hashCode(o);
        }

        @Override
        public boolean equals(Object a, Object b) {
            return Objects.equals(a, b);
        }
    };

    protected final Hash.Strategy<$Type> strategy;

    protected HashList(Hash.Strategy<$Type> strategy) {
        this.strategy = strategy;
    }

    public @NotNull Hash.Strategy<$Type> getStrategy() {
        return strategy;
    }

    public boolean isCustomStrategy() {
        return strategy != STANDARD_STRATEGY;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof List<?> other) || other.size() != size()) return false;
        Iterator<$Type> ours = iterator();
        Iterator<?> theirs = other.iterator();
        while (ours.hasNext()) {
            if (!strategy.equals(ours.next(), ($Type) theirs.next())) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for ($Type element : this) {
            hash = 31 * hash + strategy.hashCode(element);
        }
        return hash;
    }

    private abstract static sealed class ArrayBacked<$Type> extends HashList<$Type> {
        protected final $Type[] elements;

        protected ArrayBacked($Type[] elements, Hash.Strategy<$Type> strategy) {
            super(strategy);
            this.elements = elements;
        }

        @Override
        public $Type get(int index) {
            return elements[index];
        }

        @Override
        public int size() {
            return elements.length;
        }

        @Override
        public Object @NotNull [] toArray() {
            return elements.clone();
        }

        @Override
        public <$ElementType> $ElementType @NotNull [] toArray($ElementType @NotNull [] array) {
            int size = size();
            if (array.length < size) {
                return ($ElementType[]) Arrays.copyOf(elements, size, array.getClass());
            }
            System.arraycopy(elements, 0, array, 0, size);
            if (array.length > size) {
                array[size] = null;
            }
            return array;
        }
    }

    private static final class Collisions<$Type> extends ArrayBacked<$Type> {
        private static final int[] EMPTY_BOUNDS = new int[]{-1, -1};

        private final Map<$Type, int[]> firstLast;

        private Collisions($Type[] elements, Map<$Type, int[]> firstLast, Hash.Strategy<$Type> strategy) {
            super(elements, strategy);
            this.firstLast = firstLast;
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return firstLast.containsKey(o);
        }

        @Override
        public int indexOf(@Nullable Object o) {
            return firstLast.getOrDefault(o, EMPTY_BOUNDS)[0];
        }

        @Override
        public int lastIndexOf(@Nullable Object o) {
            return firstLast.getOrDefault(o, EMPTY_BOUNDS)[1];
        }
    }

    private static final class NoCollisions<$Type> extends ArrayBacked<$Type> {
        private final Object2IntMap<$Type> lookup;

        private NoCollisions($Type[] elements, Object2IntMap<$Type> lookup, Hash.Strategy<$Type> strategy) {
            super(elements, strategy);
            this.lookup = lookup;
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return lookup.containsKey(o);
        }

        @Override
        public int indexOf(@Nullable Object o) {
            return lookup.getInt(o);
        }

        @Override
        public int lastIndexOf(@Nullable Object o) {
            return indexOf(o);
        }
    }

    private static final class Empty<$Type> extends HashList<$Type> {
        private static final Empty<?> INSTANCE = new Empty<>();

        private Empty() {
            super((Hash.Strategy<$Type>) STANDARD_STRATEGY);
        }

        @Override
        public $Type get(int i) {
            throw new IndexOutOfBoundsException("Tried to grab an item from an empty HashList");
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return false;
        }

        @Override
        public int indexOf(@Nullable Object o) {
            return -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object o) {
            return -1;
        }

        @Override
        public Object @NotNull [] toArray() {
            return new Object[]{};
        }

        @Override
        public <$ElementType> $ElementType @NotNull [] toArray($ElementType @NotNull [] array) {
            if (array.length > 0) array[0] = null;
            return array;
        }
    }

    private static final class Singleton<$Type> extends HashList<$Type> {
        private final $Type value;

        Singleton($Type value, Hash.Strategy<$Type> strategy) {
            super(strategy);
            this.value = value;
        }

        @Override
        public $Type get(int i) {
            if (i == 0) return value;
            throw new IndexOutOfBoundsException("Tried to grab a non-first (0) item from a singleton HashList at index = " + i);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return strategy.equals(($Type) o, value);
        }

        @Override
        public int indexOf(@Nullable Object o) {
            return strategy.equals(($Type) o, value) ? 0 : -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object o) {
            return indexOf(o);
        }

        @Override
        public Object @NotNull [] toArray() {
            return new Object[]{value};
        }

        @Override
        public <$ElementType> $ElementType @NotNull [] toArray($ElementType @NotNull [] a) {
            if (a.length == 0) {
                $ElementType[] result = ($ElementType[]) Array.newInstance(a.getClass().getComponentType(), 1);
                result[0] = ($ElementType) value;
                return result;
            }
            a[0] = ($ElementType) value;
            if (a.length > 1) {
                a[1] = null;
            }
            return a;
        }
    }

    /**
     * Returns an empty {@code HashList}.
     * @param <$Type> the type of elements in the list
     * @return an empty, unmodifiable {@code HashList}
     */
    public static <$Type> @NotNull HashList<$Type> of() {
        return (HashList<$Type>) Empty.INSTANCE;
    }

    /**
     * Returns a {@code HashList} containing a single specified element.
     *
     * @param <$Type> the type of elements in the list
     * @param element the single element to be contained in the list
     * @return a singleton, unmodifiable {@code HashList}
     */
    public static <$Type> @NotNull HashList<$Type> of(@Nullable $Type element) {
        return new Singleton<>(element, (Hash.Strategy<$Type>) STANDARD_STRATEGY);
    }

    /**
     * Returns a {@code HashList} containing the specified elements in the order they are provided.
     * @param <$Type> the type of elements in the list
     * @param elements the elements to be contained in the list
     * @return an unmodifiable {@code HashList} containing the specified elements
     */
    @SafeVarargs
    public static <$Type> @NotNull HashList<$Type> of($Type @NotNull ... elements) {
        return fromCollection((Hash.Strategy<$Type>) STANDARD_STRATEGY, Arrays.asList(elements));
    }

    /**
     * Returns a {@code HashList} containing the elements of the specified collection,
     * in the order they are returned by the collection's iterator.
     * @param <$Type> the type of elements in the list
     * @param elements the collection whose elements are to be placed into the list
     * @return an unmodifiable {@code HashList} containing the collection's elements
     */
    public static <$Type> @NotNull HashList<$Type> of(@NotNull Collection<$Type> elements) {
        return fromCollection((Hash.Strategy<$Type>) STANDARD_STRATEGY, elements);
    }

    /**
     * Returns a {@code HashList} containing the elements of the specified iterable,
     * in the order they are returned by the iterable's iterator.
     * @param <$Type> the type of elements in the list
     * @param elements the iterable whose elements are to be placed into the list
     * @return an unmodifiable {@code HashList} containing the iterable's elements
     */
    public static <$Type> @NotNull HashList<$Type> of(@NotNull Iterable<$Type> elements) {
        return new Composer<$Type>().addAll(elements).finish();
    }

    /**
     * Returns a {@code HashList} containing a single specified element, using a custom
     * equality strategy for containment checks.
     * @param <$Type> the type of elements in the list
     * @param strategy the custom {@link Hash.Strategy} to use for equality and hashing
     * @param element the single element to be contained in the list
     * @return a singleton, unmodifiable {@code HashList} using the specified strategy
     */
    public static <$Type> @NotNull HashList<$Type> ofCustom(@NotNull Hash.Strategy<$Type> strategy, @Nullable $Type element) {
        return new Singleton<>(element, strategy);
    }

    /**
     * Returns a {@code HashList} containing the specified elements, using a custom
     * equality strategy for containment checks.
     * @param <$Type> the type of elements in the list
     * @param strategy the custom {@link Hash.Strategy} to use for equality and hashing
     * @param elements the elements to be contained in the list
     * @return an unmodifiable {@code HashList} using the specified strategy
     */
    @SafeVarargs
    public static <$Type> @NotNull HashList<$Type> ofCustom(@NotNull Hash.Strategy<$Type> strategy, $Type @NotNull ... elements) {
        return fromCollection(strategy, Arrays.asList(elements));
    }

    /**
     * Returns a {@code HashList} containing the elements of the specified collection,
     * using a custom equality strategy for containment checks.
     * @param <$Type> the type of elements in the list
     * @param strategy the custom {@link Hash.Strategy} to use for equality and hashing
     * @param elements the collection whose elements are to be placed into the list
     * @return an unmodifiable {@code HashList} using the specified strategy
     */
    public static <$Type> @NotNull HashList<$Type> ofCustom(@NotNull Hash.Strategy<$Type> strategy, @NotNull Collection<$Type> elements) {
        return fromCollection(strategy, elements);
    }

    /**
     * Returns a {@code HashList} containing the elements of the specified iterable,
     * using a custom equality strategy for containment checks.
     * @param <$Type> the type of elements in the list
     * @param strategy the custom {@link Hash.Strategy} to use for equality and hashing
     * @param elements the iterable whose elements are to be placed into the list
     * @return an unmodifiable {@code HashList} using the specified strategy
     */
    public static <$Type> @NotNull HashList<$Type> ofCustom(@NotNull Hash.Strategy<$Type> strategy, @NotNull Iterable<$Type> elements) {
        return new Composer<$Type>().containsStrategy(strategy).addAll(elements).finish();
    }

    private static <$Type> @NotNull HashList<$Type> fromCollection(@NotNull Hash.Strategy<$Type> strategy, @NotNull Collection<$Type> elements) {
        if (elements instanceof HashList<?> hl && hl.strategy == strategy) {
            return (HashList<$Type>) elements;
        }
        return switch (elements.size()) {
            case 0 -> of();
            case 1 -> new Singleton<>(elements instanceof SequencedCollection<$Type> sc ? sc.getFirst() : elements.iterator().next(), strategy);
            default -> new Composer<>(elements).containsStrategy(strategy).finish();
        };
    }

    /**
     * A builder for constructing {@link HashList} instances.
     * <p>
     * This composer allows incremental addition of elements and configuration of a custom
     * equality strategy before finalizing into an unmodifiable {@code HashList}.
     * <p>
     * <b>Thread-safety note:</b> This class is <em>thread-sensitive</em>, meaning instances
     * should not be shared across threads without external synchronization. Each builder
     * can only be used to produce a single list; calling {@link #finish()} more than once
     * will throw an {@link IllegalStateException}.
     * @param <$Type> the type of elements to be added to the list
     */
    public static final class Composer<$Type> {
        private final List<$Type> elements;
        private Hash.Strategy<$Type> strategy = (Hash.Strategy<$Type>) STANDARD_STRATEGY;
        private boolean built = false;

        /**
         * Creates a new {@code Composer} pre-populated with the elements from the specified
         * collection, in iteration order.
         * @param original the collection whose elements to initially populate the builder with
         */
        public Composer(@NotNull Collection<$Type> original) {
            this.elements = new ArrayList<>(original);
        }

        /**
         * Creates a new, empty {@code Composer}.
         */
        public Composer() {
            this.elements = new ArrayList<>();
        }

        private void checkBuilt() {
            if (built) throw new IllegalStateException("This HashList.Composer is already built");
        }

        /**
         * Sets the custom equality and hashing strategy to use for containment checks
         * in the resulting {@code HashList}.
         * @param strategy the custom {@link Hash.Strategy} to use
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_ -> this")
        public @NotNull Composer<$Type> containsStrategy(@NotNull Hash.Strategy<$Type> strategy) {
            checkBuilt();
            this.strategy = strategy;
            return this;
        }

        /**
         * Adds a single element to the list being built.
         * @param element the element to add
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_ -> this")
        public @NotNull Composer<$Type> add($Type element) {
            checkBuilt();
            this.elements.add(element);
            return this;
        }

        /**
         * Adds all specified elements to the list being built, in the order provided.
         * @param elements the elements to add
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @SafeVarargs
        @Contract("_ -> this")
        public final @NotNull Composer<$Type> addAll($Type @NotNull ... elements) {
            return addAll(Arrays.asList(elements));
        }

        /**
         * Adds all elements from the specified collection to the list being built,
         * in iteration order.
         * @param elements the collection whose elements to add
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_ -> this")
        public @NotNull Composer<$Type> addAll(@NotNull Collection<$Type> elements) {
            checkBuilt();
            this.elements.addAll(elements);
            return this;
        }

        /**
         * Adds all elements from the specified iterable to the list being built,
         * in iteration order.
         * @param iterable the iterable whose elements to add
         * @return this composer, for method chaining
         * @throws IllegalStateException if the composer has already been finalized
         */
        @Contract("_ -> this")
        public @NotNull Composer<$Type> addAll(@NotNull Iterable<? extends $Type> iterable) {
            checkBuilt();
            for ($Type element : iterable) {
                this.elements.add(element);
            }
            return this;
        }

        /**
         * Finalizes the builder and produces an unmodifiable {@code HashList} containing
         * all added elements in order.
         * @return a new, unmodifiable {@code HashList}
         * @throws IllegalStateException if the composer has already been finalized
         */
        public @NotNull HashList<$Type> finish() {
            checkBuilt();
            built = true;

            Map<$Type, int[]> map = strategy == STANDARD_STRATEGY
                    ? new Object2ObjectOpenHashMap<>()
                    : new Object2ObjectOpenCustomHashMap<>(strategy);
            boolean hasCollision = false;
            for (int i = 0; i < elements.size(); i++) {
                $Type e = elements.get(i);
                int[] bounds = map.get(e);
                if (bounds == null) {
                    map.put(e, new int[]{i, i});
                } else {
                    hasCollision = true;
                    bounds[1] = i;
                }
            }

            if (hasCollision) {
                return new Collisions<>(($Type[]) elements.toArray(), map, strategy);
            }

            return switch (elements.size()) {
                case 0 -> (HashList<$Type>) Empty.INSTANCE;
                case 1 -> new Singleton<>(elements.getFirst(), strategy);
                default -> {
                    $Type[] elementsArray = ($Type[]) elements.toArray();
                    Object2IntMap<$Type> lookup = strategy == STANDARD_STRATEGY
                            ? new Object2IntOpenHashMap<>(elementsArray.length)
                            : new Object2IntOpenCustomHashMap<>(elementsArray.length, strategy);
                    lookup.defaultReturnValue(-1);
                    for (int i = 0; i < elementsArray.length; i++) {
                        lookup.put(elementsArray[i], i);
                    }
                    yield new NoCollisions<>(elementsArray, lookup, strategy);
                }
            };
        }
    }
}