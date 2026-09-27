package ink.glowing.utils.primitive;

import ink.glowing.utils.hash.CaseInsensitive;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A boolean that can also be {@link #UNSET}, e.g. "not configured" or "any".
 */
public enum TriState {
    /**
     * The {@code true} value.
     */
    TRUE,
    /**
     * The {@code false} value.
     */
    FALSE,
    /**
     * No value: not configured, or doesn't matter.
     */
    UNSET;

    /**
     * Returns {@link #TRUE} or {@link #FALSE} for the given value.
     * @param bool the value
     * @return {@link #TRUE} if the value is {@code true}, otherwise {@link #FALSE}
     */
    @Contract(pure = true)
    public static @NotNull TriState of(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    /**
     * Returns {@link #TRUE} or {@link #FALSE} for the given value, or {@link #UNSET} for {@code null}.
     * @param bool the value, may be {@code null}
     * @return the matching state
     */
    @Contract(pure = true)
    public static @NotNull TriState of(@Nullable Boolean bool) {
        return bool == null ? UNSET : of(bool.booleanValue());
    }

    /**
     * Parses a string using {@link Mapper#DEFAULT}: case-insensitive, with common synonyms like
     * {@code yes}/{@code no} or {@code enabled}/{@code disabled}.
     * Unrecognized strings and {@code null} give {@link #UNSET}.
     * @param str the string to parse, may be {@code null}
     * @return the parsed state
     * @see Mapper
     */
    @Contract(pure = true)
    public static @NotNull TriState of(@Nullable String str) {
        return Mapper.DEFAULT.parse(str);
    }

    /**
     * Returns whether this is {@link #TRUE}.
     * @return {@code true} if this is {@link #TRUE}
     */
    @Contract(pure = true)
    public boolean isTrue() {
        return this == TRUE;
    }

    /**
     * Returns whether this is {@link #FALSE}.
     * @return {@code true} if this is {@link #FALSE}
     */
    @Contract(pure = true)
    public boolean isFalse() {
        return this == FALSE;
    }

    /**
     * Returns whether this holds a value, i.e. is not {@link #UNSET}.
     * @return {@code true} if this is {@link #TRUE} or {@link #FALSE}
     */
    @Contract(pure = true)
    public boolean isPresent() {
        return this != UNSET;
    }

    /**
     * Returns whether this is {@link #UNSET}.
     * @return {@code true} if this is {@link #UNSET}
     */
    @Contract(pure = true)
    public boolean isEmpty() {
        return this == UNSET;
    }

    /**
     * Returns the negation: {@link #TRUE} and {@link #FALSE} swap, {@link #UNSET} stays as is.
     * @return the negated state
     */
    @Contract(pure = true)
    public @NotNull TriState not() {
        return switch (this) {
            case TRUE -> FALSE;
            case FALSE -> TRUE;
            case UNSET -> UNSET;
        };
    }

    /**
     * Runs the action with the value if this is not {@link #UNSET}.
     * @param action the action to run
     */
    public void ifPresent(@NotNull BooleanConsumer action) {
        if (isPresent()) action.accept(isTrue());
    }

    /**
     * Runs the action with the value if this is not {@link #UNSET}, otherwise runs {@code emptyAction}.
     * @param action the action to run if a value is present
     * @param emptyAction the action to run if this is {@link #UNSET}
     */
    public void ifPresentOrElse(@NotNull BooleanConsumer action, @NotNull Runnable emptyAction) {
        if (isPresent()) {
            action.accept(isTrue());
        } else {
            emptyAction.run();
        }
    }

    /**
     * Returns the value.
     * @return the value
     * @throws NoSuchElementException if this is {@link #UNSET}
     */
    public boolean orElseThrow() {
        return orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    /**
     * Returns the value.
     * @param <$Exception> the type of the exception to throw
     * @param exceptionSupplier the supplier of the exception to throw if this is {@link #UNSET}
     * @return the value
     * @throws $Exception if this is {@link #UNSET}
     */
    public <$Exception extends Throwable> boolean orElseThrow(@NotNull Supplier<? extends $Exception> exceptionSupplier) throws $Exception {
        if (isEmpty()) throw exceptionSupplier.get();
        return isTrue();
    }

    /**
     * Returns the value, or {@code null} if this is {@link #UNSET}.
     * @return the value, or {@code null}
     */
    @Contract(pure = true)
    public @Nullable Boolean asBoolean() {
        return isPresent() ? isTrue() : null;
    }

    /**
     * Returns the value, or {@code fallback} if this is {@link #UNSET}.
     * @param fallback the value to return if this is {@link #UNSET}
     * @return the value, or the fallback
     */
    @Contract(pure = true)
    public boolean asBoolean(boolean fallback) {
        return isPresent() ? isTrue() : fallback;
    }

    /**
     * Returns the value, or the supplied fallback if this is {@link #UNSET}.
     * The supplier is called only when needed.
     * @param fallback the supplier of the value to return if this is {@link #UNSET}
     * @return the value, or the supplied fallback
     */
    public boolean asBoolean(@NotNull BooleanSupplier fallback) {
        return isPresent() ? isTrue() : fallback.getAsBoolean();
    }

    /**
     * {@link #UNSET} is a wildcard: it is valid for any value.
     * @param bool the value to check
     * @return {@code true} if this is {@link #UNSET} or equals the value
     * @see #isExactly(boolean)
     */
    @Contract(pure = true)
    public boolean isValidFor(boolean bool) {
        return isEmpty() || isTrue() == bool;
    }

    /**
     * {@link #UNSET} is a wildcard: it is valid for any value, including {@code null}.
     * {@link #TRUE} and {@link #FALSE} are never valid for {@code null}.
     * @param bool the value to check, may be {@code null}
     * @return {@code true} if this is {@link #UNSET} or equals the value
     * @see #isExactly(Boolean)
     */
    @Contract(pure = true)
    public boolean isValidFor(@Nullable Boolean bool) {
        return isEmpty() || bool != null && isTrue() == bool;
    }

    /**
     * Like {@link #isValidFor(boolean)}, but {@link #UNSET} is never valid.
     * @param bool the value to check
     * @return {@code true} if this equals the value
     */
    @Contract(pure = true)
    public boolean isExactly(boolean bool) {
        return this == of(bool);
    }

    /**
     * Like {@link #isValidFor(Boolean)}, but {@link #UNSET} is valid only for {@code null}.
     * @param bool the value to check, may be {@code null}
     * @return {@code true} if this is the state {@link #of(Boolean)} returns for the value
     */
    @Contract(pure = true)
    public boolean isExactly(@Nullable Boolean bool) {
        return this == of(bool);
    }

    /**
     * Converts between strings and {@link TriState}s using configurable names. Matching ignores case.
     */
    public static final class Mapper {
        /**
         * Accepts {@code true}, {@code on}, {@code yes}, {@code allow(ed)}, {@code enable(d)}
         * and their negative counterparts; anything else is {@link TriState#UNSET}.
         */
        public static final Mapper DEFAULT = builder()
                .addVariants(true, "ON", "YES", "ALLOW", "ALLOWED", "ENABLE", "ENABLED")
                .addVariants(false, "OFF", "NO", "DENY", "DENIED", "DISABLE", "DISABLED")
                .build();

        private final Map<String, TriState> lookup;
        private final Map<TriState, String> names;
        private final Map<TriState, SortedSet<String>> strings;
        private final TriState fallback;

        private Mapper(@NotNull Builder builder) {
            this.names = Collections.unmodifiableMap(new EnumMap<>(builder.names));
            this.fallback = builder.fallback;

            Map<String, TriState> lookup = CaseInsensitive.newMap();
            Map<TriState, SortedSet<String>> strings = new EnumMap<>(TriState.class);
            for (TriState state : values()) {
                SortedSet<String> set = CaseInsensitive.newLinkedSet();
                set.add(names.get(state));
                if (state != UNSET) set.addAll(builder.variants(state.isTrue()));
                for (String str : set) {
                    TriState previous = lookup.putIfAbsent(str, state);
                    if (previous != null) {
                        throw new IllegalStateException("'" + str + "' is mapped to both " + previous + " and " + state);
                    }
                }
                strings.put(state, Collections.unmodifiableSortedSet(set));
            }
            this.lookup = Collections.unmodifiableMap(lookup);
            this.strings = Collections.unmodifiableMap(strings);
        }

        /**
         * Creates a builder with {@code TRUE}, {@code FALSE} and {@code UNSET} as names, no variants
         * and {@link TriState#UNSET} as the fallback.
         * @return a new builder
         */
        @Contract(value = "-> new", pure = true)
        public static @NotNull Builder builder() {
            return new Builder();
        }

        /**
         * Maps a string to a state, ignoring case.
         * Returns the fallback for {@code null} and unknown strings.
         * @param str the string to map, may be {@code null}
         * @return the matching state, or the fallback
         */
        @Contract(pure = true)
        public @NotNull TriState parse(@Nullable String str) {
            return lookup.getOrDefault(str, fallback);
        }

        /**
         * Returns the main name of the state.
         * @param state the state
         * @return the main name
         */
        @Contract(pure = true)
        public @NotNull String name(@NotNull TriState state) {
            return names.get(state);
        }

        /**
         * Returns every string that parses to the state: its name first, then its variants.
         * The returned set is unmodifiable and case-insensitive.
         * @param state the state
         * @return the strings of the state
         */
        @Contract(pure = true)
        public @NotNull @Unmodifiable SortedSet<String> strings(@NotNull TriState state) {
            return strings.get(state);
        }

        /**
         * Returns the state used for {@code null} and unknown strings.
         * @return the fallback state
         */
        @Contract(pure = true)
        public @NotNull TriState fallback() {
            return fallback;
        }

        /**
         * Creates a builder pre-filled with this mapper's names, variants and fallback.
         * @return a new builder
         */
        @Contract(value = "-> new", pure = true)
        public @NotNull Builder toBuilder() {
            return new Builder(this);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Mapper other)) return false;
            return fallback == other.fallback
                    && names.equals(other.names)
                    && strings.equals(other.strings);
        }

        @Override
        public int hashCode() {
            return Objects.hash(names, strings, fallback);
        }

        /**
         * Builder for {@link Mapper}. Obtain via {@link Mapper#builder()}.
         */
        public static final class Builder {
            private final Map<TriState, String> names = new EnumMap<>(TriState.class);
            private final Set<String> trueVariants = CaseInsensitive.newLinkedSet();
            private final Set<String> falseVariants = CaseInsensitive.newLinkedSet();
            private TriState fallback = UNSET;

            private Builder() {
                for (TriState state : values()) {
                    names.put(state, state.name());
                }
            }

            private Builder(@NotNull Mapper mapper) {
                names.putAll(mapper.names);
                trueVariants.addAll(mapper.strings(TRUE));
                trueVariants.remove(mapper.name(TRUE));
                falseVariants.addAll(mapper.strings(FALSE));
                falseVariants.remove(mapper.name(FALSE));
                fallback = mapper.fallback;
            }

            private @NotNull Set<String> variants(boolean state) {
                return state ? trueVariants : falseVariants;
            }

            /**
             * Sets the main name of the state, used by {@link Mapper#name(TriState)}.
             * It is also recognized when parsing.
             * @param state the state
             * @param name the main name
             * @return this builder
             */
            @Contract("_, _ -> this")
            public @NotNull Builder name(@NotNull TriState state, @NotNull String name) {
                names.put(state, name);
                return this;
            }

            /**
             * Adds extra strings that parse to {@link #TRUE} or {@link #FALSE}.
             * @param state {@code true} for {@link #TRUE}, {@code false} for {@link #FALSE}
             * @param variants the strings to add
             * @return this builder
             */
            @Contract("_, _ -> this")
            public @NotNull Builder addVariants(boolean state, @NotNull String @NotNull ... variants) {
                Collections.addAll(variants(state), variants);
                return this;
            }

            /**
             * Adds extra strings that parse to {@link #TRUE} or {@link #FALSE}.
             * @param state {@code true} for {@link #TRUE}, {@code false} for {@link #FALSE}
             * @param variants the strings to add
             * @return this builder
             */
            @Contract("_, _ -> this")
            public @NotNull Builder addVariants(boolean state, @NotNull Iterable<@NotNull String> variants) {
                variants.forEach(variants(state)::add);
                return this;
            }

            /**
             * Removes all extra strings of {@link #TRUE} or {@link #FALSE}. The main name is kept.
             * @param state {@code true} for {@link #TRUE}, {@code false} for {@link #FALSE}
             * @return this builder
             */
            @Contract("_ -> this")
            public @NotNull Builder clearVariants(boolean state) {
                variants(state).clear();
                return this;
            }

            /**
             * Sets the state returned for {@code null} and unknown strings.
             * @param fallback the fallback state
             * @return this builder
             */
            @Contract("_ -> this")
            public @NotNull Builder fallback(@NotNull TriState fallback) {
                this.fallback = fallback;
                return this;
            }

            /**
             * Builds an immutable mapper.
             * @return a new mapper
             * @throws IllegalStateException if the same string is registered for different states
             */
            @Contract(value = "-> new", pure = true)
            public @NotNull Mapper build() {
                return new Mapper(this);
            }
        }
    }
}
