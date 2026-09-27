package ink.glowing.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * Helpers for searching and slicing strings and {@code char} arrays.
 */
public final class TextUtils {
    private TextUtils() { }

    /**
     * Replaces each non-overlapping occurrence of {@code search}, scanning left to right,
     * with the string produced for its index in the input.
     * @param input the string to search in
     * @param search the string to replace; if empty, nothing is replaced
     * @param indexReplacer produces the replacement for an occurrence at the given index
     * @return the input with all occurrences replaced
     */
    public static @NotNull String replaceEach(
            @NotNull String input,
            @NotNull String search,
            @NotNull IntFunction<@NotNull String> indexReplacer
    ) {
        if (search.isEmpty()) return input;
        int lastAppend = 0;
        StringBuilder builder = new StringBuilder();
        for (int index = input.indexOf(search); index != -1; index = input.indexOf(search, lastAppend)) {
            builder.append(input, lastAppend, index).append(indexReplacer.apply(index));
            lastAppend = index + search.length();
        }
        return builder.append(input, lastAppend, input.length()).toString();
    }

    /**
     * Passes the index of each non-overlapping occurrence of {@code search}, scanning left to right,
     * to the consumer.
     * @param input the string to search in
     * @param search the string to find; if empty, nothing is found
     * @param indexConsumer receives the index of each occurrence
     */
    public static void findEach(
            @NotNull String input,
            @NotNull String search,
            @NotNull IntConsumer indexConsumer
    ) {
        if (search.isEmpty()) return;
        for (int index = input.indexOf(search); index != -1; index = input.indexOf(search, index + search.length())) {
            indexConsumer.accept(index);
        }
    }

    /**
     * Returns the index of the first occurrence of the character in the array.
     * @param array the array to search in
     * @param ch the character to find
     * @return the index, or {@code -1} if not found
     */
    @Contract(pure = true)
    public static int indexOf(char @NotNull [] array, char ch) {
        return indexOf(array, ch, 0, array.length);
    }

    /**
     * Returns the index of the first occurrence of the character in the array, starting at {@code from}.
     * @param array the array to search in
     * @param ch the character to find
     * @param from the index to start from, inclusive
     * @return the index, or {@code -1} if not found
     */
    @Contract(pure = true)
    public static int indexOf(char @NotNull [] array, char ch, int from) {
        return indexOf(array, ch, from, array.length);
    }

    /**
     * Returns the index of the first occurrence of the character in the given range of the array.
     * @param array the array to search in
     * @param ch the character to find
     * @param from the index to start from, inclusive
     * @param to the index to stop at, exclusive
     * @return the index, or {@code -1} if not found
     */
    @Contract(pure = true)
    public static int indexOf(char @NotNull [] array, char ch, int from, int to) {
        for (int i = from; i < to; i++) {
            if (array[i] == ch) return i;
        }
        return -1;
    }

    /**
     * Creates a string from the characters of the array starting at {@code start}.
     * @param src the source array
     * @param start the start index, inclusive
     * @return the new string
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull String substring(char @NotNull [] src, int start) {
        return new String(src, start, src.length - start);
    }

    /**
     * Creates a string from the given range of the array.
     * @param src the source array
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @return the new string
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull String substring(char @NotNull [] src, int start, int end) {
        return new String(src, start, end - start);
    }

    /**
     * Copies the characters of the array starting at {@code start}.
     * @param src the source array
     * @param start the start index, inclusive
     * @return a new array
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static char @NotNull [] subarray(char @NotNull [] src, int start) {
        return subarray(src, start, src.length);
    }

    /**
     * Copies the given range of the array.
     * @param src the source array
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @return a new array
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static char @NotNull [] subarray(char @NotNull [] src, int start, int end) {
        return Arrays.copyOfRange(src, start, end);
    }

    /**
     * Copies the characters of the sequence starting at {@code start} into a new array.
     * @param src the source sequence
     * @param start the start index, inclusive
     * @return a new array
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static char @NotNull [] subarray(@NotNull CharSequence src, int start) {
        return subarray(src, start, src.length());
    }

    /**
     * Copies the given range of the sequence into a new array.
     * @param src the source sequence
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @return a new array
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static char @NotNull [] subarray(@NotNull CharSequence src, int start, int end) {
        char[] buf = new char[end - start];
        src.getChars(start, end, buf, 0);
        return buf;
    }
}
