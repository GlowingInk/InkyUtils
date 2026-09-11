package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

/**
 * Something that can be represented as a {@link Parameter}.
 */
public interface Parameterizable {
    /**
     * Returns this object's {@link Parameter} representation.
     * @return the corresponding {@code Parameter}
     */
    @NotNull Parameter asParameter();
}
