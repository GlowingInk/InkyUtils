package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

/**
 * Something that can be represented as a {@link Parameter}.
 * <p>
 * Implement {@link ByPlain}, {@link ByList} or {@link ByMap} to also declare which kind of
 * {@code Parameter} the object is represented as.
 */
public sealed interface Parameterizable permits Parameter, Parameterizable.ByPlain, Parameterizable.ByList, Parameterizable.ByMap {
    /**
     * Returns this object's {@link Parameter} representation.
     * @return the corresponding {@code Parameter}
     */
    @NotNull Parameter asParameter();

    /**
     * Something that is always represented as a {@link Parameter.Plain}.
     */
    non-sealed interface ByPlain extends Parameterizable {
        /**
         * Returns this object's {@link Parameter.Plain} representation.
         * @return the corresponding {@code Parameter.Plain}
         */
        @Override
        @NotNull Parameter.Plain asParameter();
    }

    /**
     * Something that is always represented as a {@link Parameter.Listed}.
     */
    non-sealed interface ByList extends Parameterizable {
        /**
         * Returns this object's {@link Parameter.Listed} representation.
         * @return the corresponding {@code Parameter.Listed}
         */
        @Override
        @NotNull Parameter.Listed asParameter();
    }

    /**
     * Something that is always represented as a {@link Parameter.Mapped}.
     */
    non-sealed interface ByMap extends Parameterizable {
        /**
         * Returns this object's {@link Parameter.Mapped} representation.
         * @return the corresponding {@code Parameter.Mapped}
         */
        @Override
        @NotNull Parameter.Mapped asParameter();
    }
}
