package ink.glowing.utils.params;

import ink.glowing.utils.hash.CaseInsensitive;
import ink.glowing.utils.params.ParameterImpl.LazyValue;
import ink.glowing.utils.params.ParameterImpl.ListedImpl;
import ink.glowing.utils.params.ParameterImpl.MappedImpl;
import ink.glowing.utils.params.ParameterImpl.PlainImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static ink.glowing.utils.params.ParameterImpl.SERIALIZED_RAW;

/**
 * A parsed parameter value: either a {@link Plain} string, a {@link Listed} sequence,
 * or a {@link Mapped} set of keyed values.
 * <p>
 * Values are whitespace-separated. Whitespace, colons and brackets in a value require quoting
 * it, and a literal {@code \} or {@code '} must be escaped:
 * <pre>{@code
 * value
 * 'quoted value'
 * escaped\ value
 * }</pre>
 * A nested {@link Listed} is wrapped in {@code [...]}, and a nested {@link Mapped} in {@code {...}}.
 * At the top level, the wrapping is omitted:
 * <pre>{@code
 * key1:value1 key2:[value2 value3] key3:{key4:value4}
 * }</pre>
 * Nesting is limited to 512 levels, changeable with the {@code ink.glowing.utils.params.maxDepth}
 * system property.
 */
public sealed interface Parameter extends Parameterizable permits Parameter.Listed, Parameter.Mapped, Parameter.Plain {
    /**
     * The index of the parameter itself, see {@link #get(int)}.
     */
    int SELF_INDEX = -1;

    /**
     * Returns the number of values held by this parameter (always {@code 1} for {@link Plain}).
     * @return the value count
     */
    int count();

    /**
     * Returns the string this parameter was parsed from, exactly as written: with quotes,
     * escapes and the wrapping brackets/braces.
     * For a parameter created with {@code of(...)}, it is {@code serialize(true)}, except for an
     * empty {@link Plain}, whose raw string is empty.
     * @return the raw value
     */
    @NotNull String raw();

    /**
     * Returns {@link #raw()} with the escaping backslashes removed, and for a {@link Plain},
     * the quotes too. The result can't be reliably parsed back, use {@link #serialize(boolean)}
     * for that.
     * @return the unescaped raw value
     */
    @NotNull String value();

    /**
     * Checks whether the other parameter holds the same values, regardless of how they were
     * written: quotes, escapes and spacing are ignored, and so is the order of {@link Mapped} entries.
     * Keys are compared ignoring case, plain values exactly, and the order of {@link Listed} entries
     * still matters.
     * @param other the parameter to compare with
     * @return {@code true} if both hold the same values
     */
    boolean matches(@NotNull Parameter other);

    /**
     * Serializes this parameter back into a normalized parameter string, which parses back
     * into the same parameter.
     * @param topLevel whether to omit the wrapping brackets/braces
     * @return the serialized form
     */
    @NotNull String serialize(boolean topLevel);

    /**
     * Looks up a nested parameter by key. For {@link Plain} and {@link Listed}, the key is an index.
     * @param key the key to look up, or {@code null} for this parameter itself
     * @return the nested parameter, or {@code null} if absent
     */
    @Nullable Parameter get(@Nullable String key);

    /**
     * Looks up a nested parameter by index. For {@link Mapped}, the index is used as a key.
     * @param index the index to look up, or {@link #SELF_INDEX} for this parameter itself
     * @return the nested parameter, or {@code null} if absent
     */
    @Nullable Parameter get(int index);

    /**
     * Looks up a nested parameter by key and maps it.
     * @param <$Result> the type of the result
     * @param key the key to look up
     * @param mapper the mapper, receiving {@code null} if the parameter is absent
     * @return the mapped result
     */
    default <$Result> @Nullable $Result map(@Nullable String key, @NotNull Function<@Nullable Parameter, ? extends $Result> mapper) {
        return mapper.apply(get(key));
    }

    /**
     * Looks up a nested parameter by index and maps it.
     * @param <$Result> the type of the result
     * @param index the index to look up
     * @param mapper the mapper, receiving {@code null} if the parameter is absent
     * @return the mapped result
     */
    default <$Result> @Nullable $Result map(int index, @NotNull Function<@Nullable Parameter, ? extends $Result> mapper) {
        return mapper.apply(get(index));
    }

    /**
     * A {@link Parameter} holding a single string, e.g. {@code value} or {@code 'quoted value'}.
     */
    sealed interface Plain extends Parameterizable.ByPlain, Parameter permits PlainImpl {
        /**
         * Returns the string, without the quotes and escapes it was written with.
         * @return the plain value
         */
        @Override
        @NotNull String value();

        /**
         * {@inheritDoc}
         */
        @Override
        default @NotNull Plain asParameter() {
            return this;
        }

        /**
         * Returns a {@code Plain} parameter of the given string.
         * @param value the string, {@code null} counts as empty
         * @return the resulting parameter
         */
        static @NotNull Plain of(@Nullable String value) {
            return value == null || value.isEmpty()
                    ? PlainImpl.EMPTY
                    : new PlainImpl(value, SERIALIZED_RAW);
        }
    }

    /**
     * A {@link Parameter} holding an ordered sequence of parameters, written as
     * {@code [value1 value2]}. They are looked up by their zero-based index.
     */
    sealed interface Listed extends Parameterizable.ByList, Parameter permits ListedImpl {
        /**
         * {@inheritDoc}
         */
        @Override
        default @NotNull Listed asParameter() {
            return this;
        }

        /**
         * Returns a {@code Listed} parameter of the given values.
         * @param value the values, {@code null} counts as empty
         * @return the resulting parameter
         */
        static @NotNull Listed of(@Nullable List<Parameter> value) {
            return value == null || value.isEmpty()
                    ? ListedImpl.EMPTY
                    : new ListedImpl(SERIALIZED_RAW, List.copyOf(value));
        }

        /**
         * Parses a {@code Listed} parameter from its top-level form, e.g. {@code value1 value2}.
         * @param inputStr the string to parse
         * @return the parsed parameter
         * @throws IllegalArgumentException if the string is malformed
         */
        static @NotNull Listed parse(@NotNull String inputStr) {
            if (inputStr.isEmpty()) return ListedImpl.EMPTY;
            char[] input = inputStr.toCharArray();
            return new ListedImpl(
                    new LazyValue(input, 0, input.length),
                    new ParserImpl(input).parseList(0)
            );
        }
    }

    /**
     * A {@link Parameter} holding keyed parameters, written as {@code {key1:value1 key2:value2}}.
     * Keys are case-insensitive and keep their written order. A repeated key keeps the first
     * spelling and the last value: {@code a:1 A:2} is {@code a:2}. A key must be directly
     * followed by its colon.
     */
    sealed interface Mapped extends Parameterizable.ByMap, Parameter permits MappedImpl {
        /**
         * {@inheritDoc}
         */
        @Override
        default @NotNull Mapped asParameter() {
            return this;
        }

        /**
         * Returns a {@code Mapped} parameter of the given entries, in their iteration order.
         * @param value the entries, {@code null} counts as empty, but they must not hold {@code null}s
         * @return the resulting parameter
         */
        static @NotNull Mapped of(@Nullable Map<String, Parameter> value) {
            if (value == null || value.isEmpty()) return MappedImpl.EMPTY;

            Map<String, Parameter> copy = CaseInsensitive.newLinkedMap(value.size());
            value.forEach((key, parameter) -> copy.put(Objects.requireNonNull(key), Objects.requireNonNull(parameter)));
            return new MappedImpl(SERIALIZED_RAW, copy);
        }

        /**
         * Parses a {@code Mapped} parameter from its top-level form, e.g. {@code key1:value1 key2:value2}.
         * @param inputStr the string to parse
         * @return the parsed parameter
         * @throws IllegalArgumentException if the string is malformed
         */
        static @NotNull Mapped parse(@NotNull String inputStr) {
            if (inputStr.isEmpty()) return MappedImpl.EMPTY;
            char[] input = inputStr.toCharArray();
            return new MappedImpl(
                    new LazyValue(input, 0, input.length),
                    new ParserImpl(input).parseMap(0)
            );
        }
    }

    /**
     * Escapes a string for use as a plain value or a key. Backslashes and quotes are escaped,
     * and the string is quoted if it is empty or has whitespace, colons or any of {@code {}[]}.
     * @param value the string to escape
     * @return the escaped string, {@code value} itself if nothing had to change
     */
    static @NotNull String escape(@NotNull String value) {
        int length = value.length();
        if (length == 0) return "''";

        boolean quote = false;
        int escapes = 0;
        for (int i = 0; i < length; i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\', '\'' -> escapes++;
                case '{', '}', '[', ']', ':' -> quote = true;
                default -> { if (Character.isWhitespace(ch)) quote = true; }
            }
        }
        if (escapes == 0 && !quote) return value;

        StringBuilder sb = new StringBuilder(length + escapes + (quote ? 2 : 0));
        if (quote) sb.append('\'');
        for (int i = 0; i < length; i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '\'') sb.append('\\');
            sb.append(ch);
        }
        if (quote) sb.append('\'');
        return sb.toString();
    }

    /**
     * Removes the escaping backslashes: each {@code \x} becomes {@code x}. Quotes are kept.
     * @param raw the string to unescape
     * @return the unescaped string, {@code raw} itself if nothing had to change
     */
    static @NotNull String unescape(@NotNull String raw) {
        int index = raw.indexOf('\\');
        if (index == -1) return raw;

        int length = raw.length();
        StringBuilder sb = new StringBuilder(length - 1).append(raw, 0, index);
        for (; index < length; index++) {
            char ch = raw.charAt(index);
            if (ch == '\\' && index + 1 < length) ch = raw.charAt(++index);
            sb.append(ch);
        }
        return sb.toString();
    }
}
