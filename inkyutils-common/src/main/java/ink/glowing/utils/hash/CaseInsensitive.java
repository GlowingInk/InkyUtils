package ink.glowing.utils.hash;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Factories for fastutil maps and sets with case-insensitive {@link String} keys.
 * Keys are compared with {@link String#equalsIgnoreCase(String)}, and {@code null} keys are allowed.
 * The original casing of the first inserted key is kept.
 */
public final class CaseInsensitive {
    private CaseInsensitive() { }

    /**
     * The shared strategy that compares strings ignoring case.
     */
    public static final Hash.Strategy<String> STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(String str) {
            if (str == null) return -1;
            final int length = str.length();
            int result = 0;
            for (int i = 0; i < length; i++) {
                result = 31 * result + Character.toLowerCase(Character.toUpperCase(str.charAt(i)));
            }
            return result;
        }

        @Override
        public boolean equals(String left, String right) {
            return left == null
                    ? right == null
                    : left.equalsIgnoreCase(right);
        }
    };

    /**
     * Creates an empty case-insensitive map that iterates in insertion order.
     * @param <$Value> the type of the map values
     * @return a new map
     */
    @Contract(value = "-> new", pure = true)
    public static <$Value> @NotNull Object2ObjectLinkedOpenCustomHashMap<String, $Value> newLinkedMap() {
        return new Object2ObjectLinkedOpenCustomHashMap<>(STRATEGY);
    }

    /**
     * Creates an empty case-insensitive map that iterates in insertion order.
     * @param <$Value> the type of the map values
     * @param expectedSize the expected number of entries
     * @return a new map
     */
    @Contract(value = "_ -> new", pure = true)
    public static <$Value> @NotNull Object2ObjectLinkedOpenCustomHashMap<String, $Value> newLinkedMap(int expectedSize) {
        return new Object2ObjectLinkedOpenCustomHashMap<>(expectedSize, STRATEGY);
    }

    /**
     * Creates a case-insensitive map that iterates in insertion order, filled with the given entries.
     * @param <$Value> the type of the map values
     * @param map the entries to copy
     * @return a new map
     */
    @Contract(value = "_ -> new", pure = true)
    public static <$Value> @NotNull Object2ObjectLinkedOpenCustomHashMap<String, $Value> newLinkedMap(@NotNull Map<String, ? extends $Value> map) {
        return new Object2ObjectLinkedOpenCustomHashMap<>(map, STRATEGY);
    }

    /**
     * Creates an empty case-insensitive map.
     * @param <$Value> the type of the map values
     * @return a new map
     */
    @Contract(value = "-> new", pure = true)
    public static <$Value> @NotNull Object2ObjectOpenCustomHashMap<String, $Value> newMap() {
        return new Object2ObjectOpenCustomHashMap<>(STRATEGY);
    }

    /**
     * Creates an empty case-insensitive map.
     * @param <$Value> the type of the map values
     * @param expectedSize the expected number of entries
     * @return a new map
     */
    @Contract(value = "_ -> new", pure = true)
    public static <$Value> @NotNull Object2ObjectOpenCustomHashMap<String, $Value> newMap(int expectedSize) {
        return new Object2ObjectOpenCustomHashMap<>(expectedSize, STRATEGY);
    }

    /**
     * Creates a case-insensitive map filled with the given entries.
     * @param <$Value> the type of the map values
     * @param map the entries to copy
     * @return a new map
     */
    @Contract(value = "_ -> new", pure = true)
    public static <$Value> @NotNull Object2ObjectOpenCustomHashMap<String, $Value> newMap(@NotNull Map<String, ? extends $Value> map) {
        return new Object2ObjectOpenCustomHashMap<>(map, STRATEGY);
    }

    /**
     * Creates an empty case-insensitive set that iterates in insertion order.
     * @return a new set
     */
    @Contract(value = "-> new", pure = true)
    public static @NotNull ObjectLinkedOpenCustomHashSet<String> newLinkedSet() {
        return new ObjectLinkedOpenCustomHashSet<>(STRATEGY);
    }

    /**
     * Creates an empty case-insensitive set that iterates in insertion order.
     * @param expectedSize the expected number of elements
     * @return a new set
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ObjectLinkedOpenCustomHashSet<String> newLinkedSet(int expectedSize) {
        return new ObjectLinkedOpenCustomHashSet<>(expectedSize, STRATEGY);
    }

    /**
     * Creates a case-insensitive set that iterates in insertion order, filled with the given strings.
     * @param collection the strings to copy
     * @return a new set
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ObjectLinkedOpenCustomHashSet<String> newLinkedSet(@NotNull Collection<String> collection) {
        return new ObjectLinkedOpenCustomHashSet<>(collection, STRATEGY);
    }

    /**
     * Creates an empty case-insensitive set.
     * @return a new set
     */
    @Contract(value = "-> new", pure = true)
    public static @NotNull ObjectOpenCustomHashSet<String> newSet() {
        return new ObjectOpenCustomHashSet<>(STRATEGY);
    }

    /**
     * Creates an empty case-insensitive set.
     * @param expectedSize the expected number of elements
     * @return a new set
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ObjectOpenCustomHashSet<String> newSet(int expectedSize) {
        return new ObjectOpenCustomHashSet<>(expectedSize, STRATEGY);
    }

    /**
     * Creates a case-insensitive set filled with the given strings.
     * @param collection the strings to copy
     * @return a new set
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ObjectOpenCustomHashSet<String> newSet(@NotNull Collection<String> collection) {
        return new ObjectOpenCustomHashSet<>(collection, STRATEGY);
    }
}
