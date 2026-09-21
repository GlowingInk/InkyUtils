package ink.glowing.utils.hash;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
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
    
    private static final Hash.Strategy<String> CI_STRATEGY = new Hash.Strategy<>() {
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
     * Returns the shared strategy that compares strings ignoring case.
     * Useful for building custom fastutil collections not covered by the factories here.
     * @return the case-insensitive strategy
     */
    public static @NotNull Hash.Strategy<String> strategy() {
        return CI_STRATEGY;
    }

    /**
     * Creates an empty case-insensitive map that iterates in insertion order.
     * @param <$Value> the type of the map values
     * @return a new map
     */
    public static <$Value> @NotNull Object2ObjectLinkedOpenCustomHashMap<String, $Value> newLinkedMap() {
        return new Object2ObjectLinkedOpenCustomHashMap<>(CI_STRATEGY);
    }

    /**
     * Creates an empty case-insensitive map that iterates in insertion order.
     * @param <$Value> the type of the map values
     * @param expectedSize the expected number of entries
     * @return a new map
     */
    public static <$Value> @NotNull Object2ObjectLinkedOpenCustomHashMap<String, $Value> newLinkedMap(int expectedSize) {
        return new Object2ObjectLinkedOpenCustomHashMap<>(expectedSize, CI_STRATEGY);
    }

    /**
     * Creates a case-insensitive map that iterates in insertion order, filled with the given entries.
     * If keys differ only in case, the later one overwrites the earlier one.
     * @param <$Value> the type of the map values
     * @param map the entries to copy
     * @return a new map
     */
    public static <$Value> @NotNull Object2ObjectLinkedOpenCustomHashMap<String, $Value> newLinkedMap(@NotNull Map<String, ? extends $Value> map) {
        return new Object2ObjectLinkedOpenCustomHashMap<>(map, CI_STRATEGY);
    }

    /**
     * Creates an empty case-insensitive map.
     * @param <$Value> the type of the map values
     * @return a new map
     */
    public static <$Value> @NotNull Object2ObjectOpenCustomHashMap<String, $Value> newMap() {
        return new Object2ObjectOpenCustomHashMap<>(CI_STRATEGY);
    }

    /**
     * Creates an empty case-insensitive map.
     * @param <$Value> the type of the map values
     * @param expectedSize the expected number of entries
     * @return a new map
     */
    public static <$Value> @NotNull Object2ObjectOpenCustomHashMap<String, $Value> newMap(int expectedSize) {
        return new Object2ObjectOpenCustomHashMap<>(expectedSize, CI_STRATEGY);
    }

    /**
     * Creates a case-insensitive map filled with the given entries.
     * If keys differ only in case, the later one overwrites the earlier one.
     * @param <$Value> the type of the map values
     * @param map the entries to copy
     * @return a new map
     */
    public static <$Value> @NotNull Object2ObjectOpenCustomHashMap<String, $Value> newMap(@NotNull Map<String, ? extends $Value> map) {
        return new Object2ObjectOpenCustomHashMap<>(map, CI_STRATEGY);
    }

    /**
     * Creates an empty case-insensitive set that iterates in insertion order.
     * @return a new set
     */
    public static @NotNull ObjectLinkedOpenCustomHashSet<String> newLinkedSet() {
        return new ObjectLinkedOpenCustomHashSet<>(CI_STRATEGY);
    }

    /**
     * Creates an empty case-insensitive set that iterates in insertion order.
     * @param expectedSize the expected number of elements
     * @return a new set
     */
    public static @NotNull ObjectLinkedOpenCustomHashSet<String> newLinkedSet(int expectedSize) {
        return new ObjectLinkedOpenCustomHashSet<>(expectedSize, CI_STRATEGY);
    }

    /**
     * Creates a case-insensitive set that iterates in insertion order, filled with the given strings.
     * If strings differ only in case, only the first one is kept.
     * @param collection the strings to copy
     * @return a new set
     */
    public static @NotNull ObjectLinkedOpenCustomHashSet<String> newLinkedSet(@NotNull Collection<String> collection) {
        return new ObjectLinkedOpenCustomHashSet<>(collection, CI_STRATEGY);
    }

    /**
     * Creates an empty case-insensitive set.
     * @return a new set
     */
    public static @NotNull ObjectOpenCustomHashSet<String> newSet() {
        return new ObjectOpenCustomHashSet<>(CI_STRATEGY);
    }

    /**
     * Creates an empty case-insensitive set.
     * @param expectedSize the expected number of elements
     * @return a new set
     */
    public static @NotNull ObjectOpenCustomHashSet<String> newSet(int expectedSize) {
        return new ObjectOpenCustomHashSet<>(expectedSize, CI_STRATEGY);
    }

    /**
     * Creates a case-insensitive set filled with the given strings.
     * If strings differ only in case, only the first one is kept.
     * @param collection the strings to copy
     * @return a new set
     */
    public static @NotNull ObjectOpenCustomHashSet<String> newSet(@NotNull Collection<String> collection) {
        return new ObjectOpenCustomHashSet<>(collection, CI_STRATEGY);
    }
}
