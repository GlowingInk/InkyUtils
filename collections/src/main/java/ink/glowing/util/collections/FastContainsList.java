package ink.glowing.util.collections;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

@Unmodifiable
public final class FastContainsList<$Element> extends AbstractList<$Element> implements RandomAccess {
    private static final int[] EMPTY_BOUNDS = new int[]{-1, -1};

    private final $Element[] elements;
    private final Map<$Element, int[]> firstLast;

    public FastContainsList(@NotNull Collection<? extends $Element> c) {
        this.elements = ($Element[]) c.toArray(new Object[0]);
        Object2ObjectOpenHashMap<$Element, int[]> firstLast = new Object2ObjectOpenHashMap<>();

        for (int i = 0; i < elements.length; i++) {
            $Element elem = elements[i];
            int[] bounds = firstLast.get(elem);
            if (bounds == null) {
                firstLast.put(elem, new int[]{i, i});
            } else {
                bounds[1] = i;
            }
        }

        firstLast.trim();
        this.firstLast = firstLast;
    }

    public FastContainsList(@NotNull Collection<? extends $Element> c, Hash.Strategy<$Element> strategy) {
        this.elements = ($Element[]) c.toArray(new Object[0]);
        Object2ObjectOpenCustomHashMap<$Element, int[]> firstLast = new Object2ObjectOpenCustomHashMap<>(strategy);

        for (int i = 0; i < elements.length; i++) {
            $Element elem = elements[i];
            int[] bounds = firstLast.get(elem);
            if (bounds == null) {
                firstLast.put(elem, new int[]{i, i});
            } else {
                bounds[1] = i;
            }
        }

        firstLast.trim();
        this.firstLast = firstLast;
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

    @Override
    public Object @NotNull [] toArray() {
        return elements.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <$ArrayElement> $ArrayElement @NotNull [] toArray($ArrayElement @NotNull [] a) {
        int size = size();
        if (a.length < size) {
            return ($ArrayElement[]) Arrays.copyOf(elements, size, a.getClass());
        }
        System.arraycopy(elements, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }
}