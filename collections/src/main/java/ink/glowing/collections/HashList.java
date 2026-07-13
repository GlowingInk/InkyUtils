package ink.glowing.collections;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

@SuppressWarnings("unchecked")
public sealed abstract class HashList<$Element> extends AbstractList<$Element> implements RandomAccess {
    private abstract static sealed class ArrayBacked<$Element> extends HashList<$Element> {
        protected final $Element[] elements;

        protected ArrayBacked($Element[] elements) {
            this.elements = elements;
        }

        @Override
        public $Element get(int index) {
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
        public <$ArrayElement> $ArrayElement @NotNull [] toArray($ArrayElement @NotNull [] array) {
            int size = size();
            if (array.length < size) {
                return ($ArrayElement[]) Arrays.copyOf(elements, size, array.getClass());
            }
            System.arraycopy(elements, 0, array, 0, size);
            if (array.length > size) {
                array[size] = null;
            }
            return array;
        }
    }

    private static final class Collisions<$Element> extends ArrayBacked<$Element> {
        private static final int[] EMPTY_BOUNDS = new int[]{-1, -1};

        private final Map<$Element, int[]> firstLast;

        private Collisions($Element[] elements, Map<$Element, int[]> firstLast) {
            super(elements);
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

    private static final class NoCollisions<$Element> extends ArrayBacked<$Element> {
        private final Object2IntMap<$Element> lookup;

        private NoCollisions($Element[] elements, Object2IntMap<$Element> lookup) {
            super(elements);
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
            return lookup.getInt(o);
        }
    }

    private static final class Empty<$Element> extends HashList<$Element> {
        private static final Empty<?> INSTANCE = new Empty<>();

        @Override
        public $Element get(int i) {
            throw new ArrayIndexOutOfBoundsException("Tried to grab an item from an empty FCL");
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
        public <$ArrayElement> $ArrayElement @NotNull [] toArray($ArrayElement @NotNull [] array) {
            if (array.length > 0) array[0] = null;
            return array;
        }
    }

    private static final class Singleton<$Element> extends HashList<$Element> {
        private final $Element value;
        private final Hash.Strategy<$Element> strategy;

        Singleton($Element value, Hash.Strategy<$Element> strategy) {
            this.value = value;
            this.strategy = strategy;
        }

        @Override
        public $Element get(int i) {
            if (i == 0) return value;
            throw new ArrayIndexOutOfBoundsException("Tried to grab an item from a singleton FCL, index = " + i);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return strategy.equals(($Element) o, value);
        }

        @Override
        public int indexOf(@Nullable Object o) {
            return strategy.equals(($Element) o, value) ? 0 : -1;
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
        public <$ArrayElement> $ArrayElement @NotNull [] toArray($ArrayElement @NotNull [] a) {
            if (a.length == 0) {
                $ArrayElement[] result = ($ArrayElement[]) Array.newInstance(a.getClass().getComponentType(), 1);
                result[0] = ($ArrayElement) value;
                return result;
            }
            a[0] = ($ArrayElement) value;
            if (a.length > 1) {
                a[1] = null;
            }
            return a;
        }
    }

    public static <$Element> @NotNull HashList<$Element> of() {
        return (HashList<$Element>) Empty.INSTANCE;
    }

    public static <$Element> @NotNull HashList<$Element> of(@Nullable $Element element) {
        return new Singleton<>(element, (Hash.Strategy<$Element>) Composer.STANDARD);
    }

    @SafeVarargs
    public static <$Element> @NotNull HashList<$Element> of($Element @NotNull ... elements) {
        return fromCollection(Arrays.asList(elements), (Hash.Strategy<$Element>) Composer.STANDARD);
    }

    public static <$Element> @NotNull HashList<$Element> of(@NotNull Collection<$Element> elements) {
        return fromCollection(elements, (Hash.Strategy<$Element>) Composer.STANDARD);
    }

    public static <$Element> @NotNull HashList<$Element> of(@NotNull Iterable<$Element> elements) {
        return new Composer<$Element>().addAll(elements).finish();
    }

    public static <$Element> @NotNull HashList<$Element> ofCustom(@Nullable $Element element, @NotNull Hash.Strategy<$Element> strategy) {
        return new Singleton<>(element, strategy);
    }

    @SafeVarargs
    public static <$Element> @NotNull HashList<$Element> ofCustom(@NotNull Hash.Strategy<$Element> strategy, $Element @NotNull ... elements) {
        return fromCollection(Arrays.asList(elements), strategy);
    }

    public static <$Element> @NotNull HashList<$Element> ofCustom(@NotNull Hash.Strategy<$Element> strategy, @NotNull Collection<$Element> elements) {
        return fromCollection(elements, strategy);
    }

    public static <$Element> @NotNull HashList<$Element> ofCustom(@NotNull Hash.Strategy<$Element> strategy, @NotNull Iterable<$Element> elements) {
        return new Composer<$Element>().containsStrategy(strategy).addAll(elements).finish();
    }

    private static <$Element> @NotNull HashList<$Element> fromCollection(@NotNull Collection<$Element> elements, @NotNull Hash.Strategy<$Element> strategy) {
        if (elements instanceof HashList<?>) {
            return (HashList<$Element>) elements;
        }
        return switch (elements.size()) {
            case 0 -> of();
            case 1 -> new Singleton<>(firstOf(elements), strategy);
            default -> new Composer<>(elements).containsStrategy(strategy).finish();
        };
    }

    private static <$Element> @Nullable $Element firstOf(@NotNull Collection<$Element> elements) {
        return elements instanceof SequencedCollection<$Element> sc ? sc.getFirst() : elements.iterator().next();
    }

    /**
     * Thread-sensitive
     */
    public static final class Composer<$Element> {
        private static final Hash.Strategy<?> STANDARD = new Hash.Strategy<>() {
            @Override
            public int hashCode(Object o) {
                return Objects.hashCode(o);
            }

            @Override
            public boolean equals(Object a, Object b) {
                return Objects.equals(a, b);
            }
        };

        private final List<$Element> elements;
        private Hash.Strategy<$Element> containsStrategy = (Hash.Strategy<$Element>) STANDARD;
        private boolean built = false;

        public Composer(@NotNull Collection<$Element> original) {
            this.elements = new ArrayList<>(original);
        }

        public Composer() {
            this.elements = new ArrayList<>();
        }

        private void checkBuilt() {
            if (built) throw new IllegalStateException("This HashList.Composer is already built; can't edit it");
        }

        @Contract("_ -> this")
        public @NotNull Composer<$Element> containsStrategy(@NotNull Hash.Strategy<$Element> strategy) {
            checkBuilt();
            this.containsStrategy = strategy;
            return this;
        }

        @Contract("_ -> this")
        public @NotNull Composer<$Element> add($Element element) {
            checkBuilt();
            this.elements.add(element);
            return this;
        }

        @SafeVarargs
        @Contract("_ -> this")
        public final @NotNull Composer<$Element> addAll($Element... elements) {
            return addAll(Arrays.asList(elements));
        }

        @Contract("_ -> this")
        public @NotNull Composer<$Element> addAll(Collection<$Element> elements) {
            checkBuilt();
            this.elements.addAll(elements);
            return this;
        }

        @Contract("_ -> this")
        public @NotNull Composer<$Element> addAll(Iterable<? extends $Element> iterable) {
            checkBuilt();
            for ($Element element : iterable) {
                this.elements.add(element);
            }
            return this;
        }

        public @NotNull HashList<$Element> finish() {
            if (built) {
                throw new IllegalStateException("This HashList.Composer is already built; can't build HashList out of it");
            }
            built = true;

            Map<$Element, int[]> map = containsStrategy == STANDARD
                    ? new Object2ObjectOpenHashMap<>()
                    : new Object2ObjectOpenCustomHashMap<>(containsStrategy);
            boolean hasCollision = false;
            for (int i = 0; i < elements.size(); i++) {
                $Element e = elements.get(i);
                int[] bounds = map.get(e);
                if (bounds == null) {
                    map.put(e, new int[]{i, i});
                } else {
                    hasCollision = true;
                    bounds[1] = i;
                }
            }

            if (hasCollision) {
                return new Collisions<>(($Element[]) elements.toArray(), map);
            }

            return switch (elements.size()) {
                case 0 -> (HashList<$Element>) Empty.INSTANCE;
                case 1 -> new Singleton<>(elements.getFirst(), containsStrategy);
                default -> {
                    $Element[] elementsArray = ($Element[]) elements.toArray();
                    Object2IntMap<$Element> lookup = containsStrategy == STANDARD
                            ? new Object2IntOpenHashMap<>(elementsArray.length)
                            : new Object2IntOpenCustomHashMap<>(elementsArray.length, containsStrategy);
                    lookup.defaultReturnValue(-1);
                    for (int i = 0; i < elementsArray.length; i++) {
                        lookup.put(elementsArray[i], i);
                    }
                    yield new NoCollisions<>(elementsArray, lookup);
                }
            };
        }
    }
}