package ink.glowing.utils.hash;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    public static @NotNull Hash.Strategy<String> strategy() {
        return CI_STRATEGY;
    }

    public static <$Value> @NotNull SortedMap<String, $Value> newLinkedMap() {
        return new Object2ObjectLinkedOpenCustomHashMap<>(CI_STRATEGY);
    }

    public static <$Value> @NotNull SortedMap<String, $Value> newLinkedMap(int expectedSize) {
        return new Object2ObjectLinkedOpenCustomHashMap<>(expectedSize, CI_STRATEGY);
    }

    public static <$Value> @NotNull SortedMap<String, $Value> newLinkedMap(@NotNull Map<String, ? extends $Value> map) {
        return new Object2ObjectLinkedOpenCustomHashMap<>(map, CI_STRATEGY);
    }

    public static <$Value> @NotNull Map<String, $Value> newMap() {
        return new Object2ObjectOpenCustomHashMap<>(CI_STRATEGY);
    }

    public static <$Value> @NotNull Map<String, $Value> newMap(int expectedSize) {
        return new Object2ObjectOpenCustomHashMap<>(expectedSize, CI_STRATEGY);
    }

    public static <$Value> @NotNull Map<String, $Value> newMap(@NotNull Map<String, ? extends $Value> map) {
        return new Object2ObjectOpenCustomHashMap<>(map, CI_STRATEGY);
    }

    public static @NotNull SortedSet<String> newLinkedSet() {
        return new ObjectLinkedOpenCustomHashSet<>(CI_STRATEGY);
    }

    public static @NotNull SortedSet<String> newLinkedSet(int expectedSize) {
        return new ObjectLinkedOpenCustomHashSet<>(expectedSize, CI_STRATEGY);
    }

    public static @NotNull SortedSet<String> newLinkedSet(@NotNull Collection<String> collection) {
        return new ObjectLinkedOpenCustomHashSet<>(collection, CI_STRATEGY);
    }

    public static @NotNull Set<String> newSet() {
        return new ObjectOpenCustomHashSet<>(CI_STRATEGY);
    }

    public static @NotNull Set<String> newSet(int expectedSize) {
        return new ObjectOpenCustomHashSet<>(expectedSize, CI_STRATEGY);
    }

    public static @NotNull Set<String> newSet(@NotNull Collection<String> collection) {
        return new ObjectOpenCustomHashSet<>(collection, CI_STRATEGY);
    }
}
