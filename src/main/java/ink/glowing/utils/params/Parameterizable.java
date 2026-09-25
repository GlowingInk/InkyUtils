package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

/**
 * Something that can be represented as a {@link Parameter}.
 * <p>
 * Implement {@link ByValue}, {@link ByList} or {@link ByMap} to also declare which kind of
 * {@code Parameter} the object is represented as.
 */
public interface Parameterizable {
    /**
     * Returns this object's {@link Parameter} representation.
     * @return the corresponding {@code Parameter}
     */
    @NotNull Parameter asParameter();

    /**
     * Something that can be represented as a {@link Parameter.Value}.
     */
    interface ByValue extends Parameterizable {
        /**
         * Returns this object's {@link Parameter.Value} representation.
         * @return the corresponding {@code Parameter.Value}
         */
        @Override
        @NotNull Parameter.Value asParameter();
    }

    /**
     * Something that can be represented as a {@link Parameter.Listed}.
     */
    interface ByList extends Parameterizable {
        /**
         * Returns this object's {@link Parameter.Listed} representation.
         * @return the corresponding {@code Parameter.Listed}
         */
        @Override
        @NotNull Parameter.Listed asParameter();
    }

    /**
     * Something that can be represented as a {@link Parameter.Mapped}.
     */
    interface ByMap extends Parameterizable {
        /**
         * Returns this object's {@link Parameter.Mapped} representation.
         * @return the corresponding {@code Parameter.Mapped}
         */
        @Override
        @NotNull Parameter.Mapped asParameter();
    }
}
