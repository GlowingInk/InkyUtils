package ink.glowing.utils.params;

import ink.glowing.utils.params.ParameterImpl.LazyValue;
import ink.glowing.utils.params.ParameterImpl.ListedImpl;
import ink.glowing.utils.params.ParameterImpl.MappedImpl;
import ink.glowing.utils.params.ParameterImpl.PlainImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static ink.glowing.utils.params.ParameterImpl.GLOBAL_VALUE;

/**
 * A parsed parameter value: either a {@link Plain} string, a {@link Listed} sequence,
 * or a {@link Mapped} set of keyed values.
 * <p>
 * Values are whitespace-separated. A value containing whitespace must be quoted, and a
 * literal {@code \} or {@code '} inside it must be escaped:
 * <pre>{@code
 * value
 * 'quoted value'
 * escaped\ value
 * }</pre>
 * A nested {@link Listed} is wrapped in {@code [...]}, and a nested {@link Mapped} in
 * {@code {...}}:
 * <pre>{@code
 * [value1 value2]
 * {key1:value1 key2:value2}
 * }</pre>
 * At the top level ({@code main == true} in {@link #asValue(boolean)}), the
 * wrapping brackets/braces are omitted, so the same values read as:
 * <pre>{@code
 * value1 value2
 * key1:value1 key2:value2
 * }</pre>
 * Nesting is unrestricted, e.g. {@code key:[value1 {key2:value2}]}.
 */
public sealed interface Parameter extends Parameterizable permits Parameter.Listed, Parameter.Mapped, Parameter.Plain {
    /**
     * The index passed to {@link #get(int)}/{@link #get(String)} to get this parameter itself.
     */
    int SELF_INDEX = -1;

    /**
     * Returns the number of values held by this parameter (always {@code 1} for {@link Plain}).
     * @return the value count
     */
    int count();

    /**
     * Returns the raw, but unescaped string this parameter was parsed from.
     * @return the raw value
     */
    @NotNull String view();

    /**
     * Serializes this parameter back into its parameter-string form.
     * @param main whether this is the top-level parameter, omitting the wrapping brackets/braces
     * @return the serialized form
     */
    @NotNull String asValue(boolean main);

    /**
     * Looks up a nested parameter by key.
     * @param key the key to look up, or {@code null} to return this parameter itself
     * @return the nested parameter, or {@code null} if absent
     */
    @Nullable Parameter get(@Nullable String key);

    /**
     * Looks up a nested parameter by index.
     * @param index the index to look up, or {@link #SELF_INDEX} to return this parameter itself
     * @return the nested parameter, or {@code null} if absent
     */
    @Nullable Parameter get(int index);

    /**
     * Looks up a nested parameter by key and applies a mapper to the result.
     * @param key the key to look up
     * @param mapper the mapper applied to the looked-up parameter, which may be {@code null}
     * @return the mapped result
     */
    default <$Result> @Nullable $Result map(@Nullable String key, @NotNull Function<@Nullable Parameter, ? extends $Result> mapper) {
        return mapper.apply(get(key));
    }

    /**
     * Looks up a nested parameter by index and applies a mapper to the result.
     * @param index the index to look up
     * @param mapper the mapper applied to the looked-up parameter, which may be {@code null}
     * @return the mapped result
     */
    default <$Result> @Nullable $Result map(int index, @NotNull Function<@Nullable Parameter, ? extends $Result> mapper) {
        return mapper.apply(get(index));
    }

    /**
     * A {@link Parameter} holding a single plain string value, e.g. {@code value} or
     * {@code 'quoted value'}. Its {@link #count()} is always {@code 1} and {@link #get}
     * only accepts {@link #SELF_INDEX}/{@code "-1"} or {@code 0}/{@code "0"}, returning itself.
     */
    sealed interface Plain extends Parameterizable.ByPlain, Parameter permits PlainImpl {
        /**
         * {@inheritDoc}
         */
        @Override
        default @NotNull Plain asParameter() {
            return this;
        }

        /**
         * Returns a {@code Plain} parameter wrapping the given value.
         * @param value the value to wrap, may be {@code null} or empty
         * @return the resulting parameter
         */
        static @NotNull Plain of(@Nullable String value) {
            return value == null || value.isEmpty()
                    ? PlainImpl.EMPTY
                    : new PlainImpl(value);
        }
    }

    /**
     * A {@link Parameter} holding an ordered sequence of nested parameters, written as
     * {@code [value1 value2 ...]} (brackets omitted at the top level). Entries are looked
     * up by their zero-based position, e.g. {@code get(0)} for {@code value1}.
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
         * Returns a {@code Listed} parameter wrapping the given values.
         * @param value the values to wrap, may be {@code null} or empty
         * @return the resulting parameter
         */
        static @NotNull Listed of(@Nullable List<Parameter> value) {
            return value == null || value.isEmpty()
                    ? ListedImpl.EMPTY
                    : new ListedImpl(GLOBAL_VALUE, List.copyOf(value));
        }

        /**
         * Parses a {@code Listed} parameter from its string form, e.g. {@code [value1 value2]}
         * or the bare {@code value1 value2} at the top level.
         * @param inputStr the string to parse
         * @return the parsed parameter
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
     * A {@link Parameter} holding a set of keyed nested parameters, written as
     * {@code {key1:value1 key2:value2 ...}} (braces omitted at the top level). Entries
     * are looked up by their key, e.g. {@code get("key1")}.
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
         * Returns a {@code Mapped} parameter wrapping the given entries.
         * @param value the entries to wrap, may be {@code null} or empty
         * @return the resulting parameter
         */
        static @NotNull Mapped of(@Nullable Map<String, Parameter> value) {
            return value == null || value.isEmpty()
                    ? MappedImpl.EMPTY
                    : new MappedImpl(GLOBAL_VALUE, Map.copyOf(value));
        }

        /**
         * Parses a {@code Mapped} parameter from its string form, e.g. {@code {key1:value1 key2:value2}}
         * or the bare {@code key1:value1 key2:value2} at the top level.
         * @param inputStr the string to parse
         * @return the parsed parameter
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
     * Escapes a plain value for use in a parameter string, quoting it if it contains whitespace or
     * a colon or any of the map/list bracket symbols {@code {}[]}. An empty value is written as {@code ''}.
     * @param value the value to escape
     * @return the escaped value
     */
    static @NotNull String escapePlainValue(@NotNull String value) {
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
}
