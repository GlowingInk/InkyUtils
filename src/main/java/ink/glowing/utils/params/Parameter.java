package ink.glowing.utils.params;

import ink.glowing.utils.EnumUtils;
import ink.glowing.utils.hash.CaseInsensitive;
import ink.glowing.utils.params.ParameterEditorImpl.NoopEditor;
import ink.glowing.utils.primitive.TriState;
import ink.glowing.utils.primitive.num.NumberUtils;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.*;

import static ink.glowing.utils.params.ParameterHelper.isAbsent;

/**
 * A parsed parameter value: either a plain string, a list ({@link #isList()}) of parameters,
 * or a map ({@link #isMap()}) of keyed parameters.
 * <p>
 * Lookups never return {@code null}: an absent parameter is the {@linkplain #missing() missing} one,
 * which is safe to keep chaining lookups on.
 * <p>
 * Values are whitespace-separated. Whitespace, colons and brackets in a value require quoting
 * it, and a literal {@code \} or {@code '} must be escaped:
 * <pre>{@code
 * value
 * 'quoted value'
 * escaped\ value
 * }</pre>
 * A nested list is wrapped in {@code [...]}, and a nested map in {@code {...}}.
 * At the top level, the wrapping is omitted:
 * <pre>{@code
 * key1:value1 key2:[value2 value3] key3:{key4:value4}
 * }</pre>
 * Nesting is limited to {@link #MAX_DEPTH} levels.
 */
public sealed interface Parameter extends Parameterizable permits MissingImpl, ValueImpl, CompoundImpl {
    /**
     * The name of the system property that overrides {@link #MAX_DEPTH}.
     */
    String MAX_DEPTH_PROPERTY = "ink.glowing.utils.params.maxDepth";

    /**
     * The maximum nesting depth of a parsed parameter: {@code 512} by default, or the value of the
     * {@link #MAX_DEPTH_PROPERTY} system property if set (clamped to {@code >=1}), read once when this
     * interface is initialized. Deeper input fails to parse with an {@link IllegalArgumentException}.
     */
    int MAX_DEPTH = Math.max(1, Integer.getInteger(MAX_DEPTH_PROPERTY, 512));

    /**
     * Returns the number of values held by this parameter (always {@code 1} for a plain value).
     * @return the value count
     */
    @Contract(pure = true)
    int count();

    /**
     * Returns the keys {@link #get(String)} finds a nested parameter by: for a map, its keys
     * in their written order, for a list, its indexes, and for a plain value, a single {@code "0"}.
     * @return the unmodifiable set of keys
     */
    @Contract(pure = true)
    @NotNull @Unmodifiable Set<String> keys();

    /**
     * Returns the string this parameter was parsed from, exactly as written: with quotes,
     * escapes and the wrapping brackets/braces.
     * For a parameter created with {@code ofValue(...)}, {@code ofList(...)} or {@code ofMap(...)}, it is {@code serialize(true)}, except for an
     * empty plain value, whose raw string is empty.
     * @return the raw value
     */
    @Contract(pure = true)
    @NotNull String raw();

    /**
     * Returns {@link #raw()} with the escaping backslashes removed, and for a plain value,
     * the quotes too. The result can't be reliably parsed back, use {@link #serialize(boolean)}
     * for that.
     * @return the unescaped raw value
     */
    @Contract(pure = true)
    @NotNull String textValue();

    /**
     * Checks whether the other parameter holds the same values, regardless of how they were
     * written: quotes, escapes and spacing are ignored, and so is the order of map entries.
     * Keys are compared ignoring case, plain values exactly, and the order of list entries
     * still matters.
     * @param other the parameter to compare with
     * @return {@code true} if both hold the same values
     */
    @Contract(pure = true)
    boolean matches(@NotNull Parameter other);

    /**
     * Serializes this parameter back into a normalized parameter string, which parses back
     * into the same parameter.
     * @param topLevel whether to omit the wrapping brackets/braces
     * @return the serialized form
     */
    @Contract(pure = true)
    @NotNull String serialize(boolean topLevel);

    /**
     * Returns whether this is a list, written as {@code [value1 value2]} and looked up by
     * zero-based index.
     * @return {@code true} if this is a list
     */
    @Contract(pure = true)
    default boolean isList() {
        return false;
    }

    /**
     * Returns whether this is a map, written as {@code {key1:value1 key2:value2}}.
     * Keys are case-insensitive and keep their written order. A repeated key keeps the first
     * spelling and the last value: {@code a:1 A:2} is {@code a:2}. A key must be directly
     * followed by its colon.
     * @return {@code true} if this is a map
     */
    @Contract(pure = true)
    default boolean isMap() {
        return false;
    }

    /**
     * Returns whether this is a plain value, i.e. neither a list, a map, nor {@link #missing()}.
     * @return {@code true} if this is a plain value
     */
    @Contract(pure = true)
    default boolean isPlain() {
        return false;
    }

    /**
     * Returns whether this is the {@link #missing()} parameter, i.e. a lookup found nothing.
     * @return {@code true} if this is {@link #missing()}
     */
    @Contract(pure = true)
    default boolean isMissing() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Contract(value = "-> this", pure = true)
    default @NotNull @Unmodifiable Parameter asParameter() {
        return this;
    }

    /**
     * Returns the parameter that stands for an absent one: what a lookup finds when there is nothing,
     * and the {@code null} of this API. It holds no values, has an empty text, and every lookup on it
     * gives it again. Use {@link #isMissing()} to tell it from an empty plain value.
     * <p>
     * {@code ofList(...)} and {@code ofMap(...)} skip it, as they do {@code null}.
     * @return the missing parameter
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter missing() {
        return MissingImpl.INSTANCE;
    }

    /**
     * Looks up a nested parameter by key. For a plain value and a list, the key is an index.
     * @param key the key to look up
     * @return the nested parameter, or {@link #missing()} if absent
     */
    @Contract(pure = true)
    @NotNull @Unmodifiable Parameter get(@NotNull String key);

    /**
     * Looks up a nested parameter by index. For a map, the index is used as a key.
     * @param index the index to look up
     * @return the nested parameter, or {@link #missing()} if absent
     */
    @Contract(pure = true)
    @NotNull @Unmodifiable Parameter get(int index);

    /**
     * Returns this parameter as an {@link Optional}.
     * @return this parameter, or empty if it is {@link #missing()}
     */
    @Contract(pure = true)
    default @NotNull Optional<Parameter> toOptional() {
        return isMissing() ? Optional.empty() : Optional.of(this);
    }

    /**
     * Looks up a nested parameter by key, as an {@link Optional}.
     * @param key the key to look up
     * @return the nested parameter, or empty if it is {@link #missing()}
     */
    @Contract(pure = true)
    default @NotNull Optional<Parameter> find(@NotNull String key) {
        return get(key).toOptional();
    }

    /**
     * Looks up a nested parameter by index, as an {@link Optional}.
     * @param index the index to look up
     * @return the nested parameter, or empty if it is {@link #missing()}
     */
    @Contract(pure = true)
    default @NotNull Optional<Parameter> find(int index) {
        return get(index).toOptional();
    }

    /**
     * Checks whether a nested parameter exists for the key.
     * @param key the key to look up
     * @return {@code true} if {@link #get(String)} finds a parameter, i.e. not {@link #missing()}
     */
    @Contract(pure = true)
    default boolean contains(@NotNull String key) {
        return !get(key).isMissing();
    }

    /**
     * Checks whether a nested parameter exists at the index.
     * @param index the index to look up
     * @return {@code true} if {@link #get(int)} finds a parameter, i.e. not {@link #missing()}
     */
    @Contract(pure = true)
    default boolean contains(int index) {
        return !get(index).isMissing();
    }

    /**
     * Returns a copy of this map with the key set. The key is matched ignoring case, and an existing
     * key keeps its spelling and position. Any other kind of parameter is returned as is.
     * @param key the key to set
     * @param value the value, an absent one ({@code null} or {@link #missing()}) removes the key
     * @return the resulting map, or this if nothing changes
     */
    @Contract(pure = true)
    default @NotNull @Unmodifiable Parameter with(@NotNull String key, @Nullable Parameter value) {
        return this;
    }

    /**
     * Returns a copy of this map with the entries set, as by {@link #with(String, Parameter)}.
     * Any other kind of parameter is returned as is.
     * @param entries the entries, {@code null} keys are skipped
     * @return the resulting map, or this if nothing changes
     */
    @Contract(pure = true)
    default @NotNull @Unmodifiable Parameter with(@NotNull Map<String, Parameter> entries) {
        return this;
    }

    /**
     * Returns a copy of this list with the entry at the index set. An index past the end extends
     * the list, filling the gap with {@link #missing()}. Any other kind of parameter is returned as is.
     * <p>
     * The memory taken grows with the index, so bound it if it's untrusted.
     * @param index the zero-based index, a negative one changes nothing
     * @param value the value, an absent one blanks an existing entry and never extends the list
     * @return the resulting list, or this if nothing changes
     */
    @Contract(pure = true)
    default @NotNull @Unmodifiable Parameter with(int index, @Nullable Parameter value) {
        return this;
    }

    /**
     * Returns a copy of this list with the pairs set, as by {@link #with(int, Parameter)}.
     * Any other kind of parameter is returned as is.
     * <p>
     * The memory taken grows with the highest index, so bound them if they're untrusted.
     * @param pairs the index and value pairs, {@code null} pairs and negative indexes are skipped
     * @return the resulting list, or this if nothing changes
     */
    @Contract(pure = true)
    default @NotNull @Unmodifiable Parameter with(@NotNull Collection<? extends IntObjectPair<Parameter>> pairs) {
        return this;
    }

    /**
     * Returns a copy of this list with the value inserted at the index, shifting the later entries.
     * An index past the end first extends the list with {@link #missing()}. Any other kind of parameter
     * is returned as is.
     * <p>
     * The memory taken grows with the index, so bound it if it's untrusted.
     * @param index the zero-based index, a negative one changes nothing
     * @param value the value, an absent one changes nothing
     * @return the resulting list, or this if nothing changes
     */
    @Contract(pure = true)
    default @NotNull @Unmodifiable Parameter withInsert(int index, @Nullable Parameter value) {
        return this;
    }

    /**
     * Returns a copy of this list with each value inserted right after the entry at its index, shifting
     * the later ones. The indexes refer to this list, and values with the same index keep their order.
     * An index past the end first extends the list with {@link #missing()}. Any other kind of parameter
     * is returned as is.
     * <p>
     * The memory taken grows with the highest index, so bound them if they're untrusted.
     * @param pairs the index and value pairs, {@code null} pairs, negative indexes and absent values are skipped
     * @return the resulting list, or this if nothing changes
     */
    @Contract(pure = true)
    default @NotNull @Unmodifiable Parameter withInsert(@NotNull Collection<? extends IntObjectPair<Parameter>> pairs) {
        return this;
    }

    /**
     * Starts a chain of edits of this list, for what {@code with} can't express, like removing entries.
     * Any other kind of parameter gets an editor that ignores the edits.
     * @return the list editor
     */
    @Contract(pure = true)
    default @NotNull ParameterEditor.OfList editList() {
        return new NoopEditor(this);
    }

    /**
     * Starts a chain of edits of this map, for what {@code with} can't express, like removing keys.
     * Any other kind of parameter gets an editor that ignores the edits.
     * @return the map editor
     */
    @Contract(pure = true)
    default @NotNull ParameterEditor.OfMap editMap() {
        return new NoopEditor(this);
    }

    /**
     * Looks up a nested parameter by key and maps it.
     * @param <$Result> the type of the result
     * @param key the key to look up
     * @param mapper the mapper, receiving {@link #missing()} if the parameter is absent
     * @return the mapped result
     */
    default <$Result> $Result getMapped(@NotNull String key, @NotNull Function<@NotNull Parameter, ? extends $Result> mapper) {
        return mapper.apply(get(key));
    }

    /**
     * Looks up a nested parameter by index and maps it.
     * @param <$Result> the type of the result
     * @param index the index to look up
     * @param mapper the mapper, receiving {@link #missing()} if the parameter is absent
     * @return the mapped result
     */
    default <$Result> $Result getMapped(int index, @NotNull Function<@NotNull Parameter, ? extends $Result> mapper) {
        return mapper.apply(get(index));
    }

    /**
     * Looks up a nested parameter by key and returns its {@link #textValue()}.
     * @param key the key to look up
     * @return the text value, an empty string if the parameter is absent
     */
    default @NotNull String getText(@NotNull String key) {
        return get(key).textValue();
    }

    /**
     * Looks up a nested parameter by index and returns its {@link #textValue()}.
     * @param index the index to look up
     * @return the text value, an empty string if the parameter is absent
     */
    default @NotNull String getText(int index) {
        return get(index).textValue();
    }

    /**
     * Maps this parameter's {@link #textValue()}.
     * @param <$Result> the type of the result
     * @param mapper the mapper, receiving an empty string for {@link #missing()}
     * @return the mapped result
     */
    default <$Result> $Result as(@NotNull Function<@NotNull String, ? extends $Result> mapper) {
        return mapper.apply(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as an {@code int}.
     * @return the parsed value, or empty if the value is absent or malformed
     * @see NumberUtils#parseInt(String)
     */
    default @NotNull OptionalInt asInt() {
        return NumberUtils.parseInt(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as an {@code int}.
     * @param def the fallback, used if the value is absent or malformed
     * @return the parsed value, or the fallback
     * @see NumberUtils#parseInt(String, int)
     */
    default int asInt(int def) {
        return NumberUtils.parseInt(textValue(), def);
    }

    /**
     * Parses this parameter's {@link #textValue()} as an {@code int}, with a lazily computed fallback.
     * @param def the supplier of the fallback, called only if the value is absent or malformed
     * @return the parsed value, or the fallback
     * @see NumberUtils#parseInt(String, IntSupplier)
     */
    default int asInt(@NotNull IntSupplier def) {
        return NumberUtils.parseInt(textValue(), def);
    }

    /**
     * Maps this parameter's {@link #textValue()} to an {@code int}.
     * @param mapper the mapper, receiving an empty string for {@link #missing()}
     * @return the mapped result
     */
    default int asInt(@NotNull ToIntFunction<@NotNull String> mapper) {
        return mapper.applyAsInt(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code long}.
     * @return the parsed value, or empty if the value is absent or malformed
     * @see NumberUtils#parseLong(String)
     */
    default @NotNull OptionalLong asLong() {
        return NumberUtils.parseLong(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code long}.
     * @param def the fallback, used if the value is absent or malformed
     * @return the parsed value, or the fallback
     * @see NumberUtils#parseLong(String, long)
     */
    default long asLong(long def) {
        return NumberUtils.parseLong(textValue(), def);
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code long}, with a lazily computed fallback.
     * @param def the supplier of the fallback, called only if the value is absent or malformed
     * @return the parsed value, or the fallback
     * @see NumberUtils#parseLong(String, LongSupplier)
     */
    default long asLong(@NotNull LongSupplier def) {
        return NumberUtils.parseLong(textValue(), def);
    }

    /**
     * Maps this parameter's {@link #textValue()} to a {@code long}.
     * @param mapper the mapper, receiving an empty string for {@link #missing()}
     * @return the mapped result
     */
    default long asLong(@NotNull ToLongFunction<@NotNull String> mapper) {
        return mapper.applyAsLong(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code double}.
     * @return the parsed value, or empty if the value is absent or malformed
     * @see NumberUtils#parseDouble(String)
     */
    default @NotNull OptionalDouble asDouble() {
        return NumberUtils.parseDouble(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code double}.
     * @param def the fallback, used if the value is absent or malformed
     * @return the parsed value, or the fallback
     * @see NumberUtils#parseDouble(String, double)
     */
    default double asDouble(double def) {
        return NumberUtils.parseDouble(textValue(), def);
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code double}, with a lazily computed fallback.
     * @param def the supplier of the fallback, called only if the value is absent or malformed
     * @return the parsed value, or the fallback
     * @see NumberUtils#parseDouble(String, DoubleSupplier)
     */
    default double asDouble(@NotNull DoubleSupplier def) {
        return NumberUtils.parseDouble(textValue(), def);
    }

    /**
     * Maps this parameter's {@link #textValue()} to a {@code double}.
     * @param mapper the mapper, receiving an empty string for {@link #missing()}
     * @return the mapped result
     */
    default double asDouble(@NotNull ToDoubleFunction<@NotNull String> mapper) {
        return mapper.applyAsDouble(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@link TriState}: case-insensitive,
     * with synonyms like {@code yes}/{@code no} or {@code enabled}/{@code disabled}.
     * @return the parsed state, {@link TriState#UNSET} if the value is absent or not recognized
     * @see TriState#of(String)
     */
    default @NotNull TriState asTriState() {
        return TriState.of(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code boolean}: {@code true} or {@code false},
     * ignoring case. Same as {@code asBoolean(false)}.
     * @return the parsed value, or {@code false} if the value is absent or not recognized
     * @see #asTriState()
     */
    default boolean asBoolean() {
        return asBoolean(false);
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code boolean}: {@code true} or {@code false},
     * ignoring case.
     * @param def the fallback, used if the value is absent or not recognized
     * @return the parsed value, or the fallback
     * @see #asTriState()
     */
    default boolean asBoolean(boolean def) {
        Boolean parsed = parseBoolean(textValue());
        return parsed != null ? parsed : def;
    }

    /**
     * Parses this parameter's {@link #textValue()} as a {@code boolean}, with a lazily computed fallback.
     * @param def the supplier of the fallback, called only if the value is absent or not recognized
     * @return the parsed value, or the fallback
     * @see #asBoolean(boolean)
     */
    default boolean asBoolean(@NotNull BooleanSupplier def) {
        Boolean parsed = parseBoolean(textValue());
        return parsed != null ? parsed : def.getAsBoolean();
    }

    /**
     * Maps this parameter's {@link #textValue()} to a {@code boolean}.
     * @param mapper the mapper, receiving an empty string for {@link #missing()}
     * @return the mapped result
     */
    default boolean asBoolean(@NotNull Predicate<@NotNull String> mapper) {
        return mapper.test(textValue());
    }

    /**
     * Parses this parameter's {@link #textValue()} as a constant of the enum, ignoring case.
     * @param <$Enum> the type of the enum
     * @param type the enum class
     * @return the matching constant, or empty if the value is absent or matches no constant
     * @see EnumUtils#asEnum(String, Class)
     */
    default <$Enum extends Enum<$Enum>> @NotNull Optional<$Enum> asEnum(@NotNull Class<$Enum> type) {
        return EnumUtils.asEnum(textValue(), type);
    }

    /**
     * Parses this parameter's {@link #textValue()} as a constant of the enum, ignoring case.
     * @param <$Enum> the type of the enum
     * @param type the enum class
     * @param def the fallback, may be {@code null}, used if the value is absent or matches no constant
     * @return the matching constant, or the fallback
     * @see EnumUtils#asEnum(String, Class, Enum)
     */
    @Contract(value = "_, !null -> !null", pure = true)
    default <$Enum extends Enum<$Enum>> @Nullable $Enum asEnum(@NotNull Class<$Enum> type, @Nullable $Enum def) {
        return EnumUtils.asEnum(textValue(), type, def);
    }

    /**
     * Parses this parameter's {@link #textValue()} as a constant of the fallback's enum, ignoring case.
     * @param <$Enum> the type of the enum
     * @param def the fallback, which also gives the enum, used if the value is absent or matches no constant
     * @return the matching constant, or the fallback
     * @see EnumUtils#asEnum(String, Enum)
     */
    default <$Enum extends Enum<$Enum>> @NotNull $Enum asEnum(@NotNull $Enum def) {
        return EnumUtils.asEnum(textValue(), def);
    }

    /**
     * Returns an empty plain value parameter.
     * @return the empty parameter
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofValue() {
        return StringImpl.EMPTY;
    }

    /**
     * Returns a plain value parameter of the given string.
     * @param value the string, {@code null} counts as empty
     * @return the resulting parameter
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofValue(@Nullable String value) {
        return value == null || value.isEmpty()
                ? StringImpl.EMPTY
                : new StringImpl(value);
    }

    /**
     * Returns a plain value parameter of the given number, which {@code asInt()} returns without parsing.
     * @param value the number
     * @return the resulting parameter
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofValue(int value) {
        return new IntImpl(value);
    }

    /**
     * Returns a plain value parameter of the given number, which {@code asLong()} returns without parsing.
     * @param value the number
     * @return the resulting parameter
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofValue(long value) {
        return new LongImpl(value);
    }

    /**
     * Returns a plain value parameter of the given number, which {@code asDouble()} returns without parsing.
     * @param value the number
     * @return the resulting parameter
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofValue(double value) {
        return new DoubleImpl(value);
    }

    /**
     * Returns a plain value parameter of the given boolean, written as {@code true} or {@code false}.
     * @param value the boolean
     * @return the resulting parameter
     * @see #asTriState()
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofValue(boolean value) {
        return BooleanImpl.of(TriState.of(value));
    }

    /**
     * Returns a plain value parameter of the given state, written as {@code true}, {@code false} or {@code unset}.
     * @param value the state, {@code null} counts as empty
     * @return the resulting parameter
     * @see #asTriState()
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofValue(@Nullable TriState value) {
        return value == null ? StringImpl.EMPTY : BooleanImpl.of(value);
    }

    /**
     * Returns a plain value parameter of the given enum constant, written as its name.
     * @param value the constant, {@code null} counts as empty
     * @return the resulting parameter
     * @see #asEnum(Class)
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofValue(@Nullable Enum<?> value) {
        return value == null ? StringImpl.EMPTY : new EnumImpl(value);
    }

    /**
     * Returns an empty list parameter.
     * @return the empty parameter
     * @see #isList()
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofList() {
        return ListedImpl.EMPTY;
    }

    /**
     * Returns a list parameter of the given values.
     * @param value the values, {@code null} counts as empty, {@code null} and {@link #missing()} ones are skipped
     * @return the resulting parameter
     * @see #isList()
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofList(@Nullable List<Parameter> value) {
        if (value == null || value.isEmpty()) return ListedImpl.EMPTY;

        ArrayList<Parameter> copy = new ArrayList<>(value.size());
        for (Parameter parameter : value) {
            if (!isAbsent(parameter)) copy.add(parameter);
        }
        copy.trimToSize();
        return new ListedImpl(copy);
    }

    /**
     * Returns an empty map parameter.
     * @return the empty parameter
     * @see #isMap()
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofMap() {
        return MappedImpl.EMPTY;
    }

    /**
     * Returns a map parameter of the given entries, in their iteration order.
     * @param value the entries, {@code null} keys and {@code null} or {@link #missing()} values are skipped
     * @return the resulting parameter
     * @see #isMap()
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter ofMap(@Nullable Map<String, Parameter> value) {
        if (value == null || value.isEmpty()) return MappedImpl.EMPTY;

        var copy = CaseInsensitive.<Parameter>newLinkedMap(value.size());
        for (Map.Entry<String, Parameter> entry : value.entrySet()) {
            String key = entry.getKey();
            if (key != null && !isAbsent(entry.getValue())) copy.put(key, entry.getValue());
        }
        return new MappedImpl(copy);
    }

    /**
     * Parses a list parameter from its top-level form, e.g. {@code value1 value2}.
     * @param inputStr the string to parse
     * @return the parsed parameter
     * @throws IllegalArgumentException if the string is malformed
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter parseList(@NotNull String inputStr) {
        if (inputStr.isEmpty()) return ListedImpl.EMPTY;
        char[] input = inputStr.toCharArray();
        return new ListedImpl(
                new LazyValue(input, 0, input.length),
                new ParserImpl(input).parseList(0)
        );
    }

    /**
     * Parses a map parameter from its top-level form, e.g. {@code key1:value1 key2:value2}.
     * @param inputStr the string to parse
     * @return the parsed parameter
     * @throws IllegalArgumentException if the string is malformed
     */
    @Contract(pure = true)
    static @NotNull @Unmodifiable Parameter parseMap(@NotNull String inputStr) {
        if (inputStr.isEmpty()) return MappedImpl.EMPTY;
        char[] input = inputStr.toCharArray();
        return new MappedImpl(
                new LazyValue(input, 0, input.length),
                new ParserImpl(input).parseMap(0)
        );
    }

    private static @Nullable Boolean parseBoolean(@NotNull String text) {
        if (text.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (text.equalsIgnoreCase("false")) return Boolean.FALSE;
        return null;
    }

    /**
     * Escapes a string for use as a plain value or a key. Backslashes and quotes are escaped,
     * and the string is quoted if it is empty or has whitespace, colons or any of {@code {}[]}.
     * @param value the string to escape
     * @return the escaped string, {@code value} itself if nothing had to change
     */
    @Contract(pure = true)
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
    @Contract(pure = true)
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
