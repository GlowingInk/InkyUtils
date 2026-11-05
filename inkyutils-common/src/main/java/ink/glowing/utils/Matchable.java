package ink.glowing.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * An object that can be compared to another by the values it holds. This is looser than
 * {@link Object#equals(Object)}: objects that were written or typed differently can still match.
 * @param <$Type> the type of objects to compare with
 */
public interface Matchable<$Type> {
    /**
     * Checks whether the other object holds the same values as this one.
     * @param other the object to compare with
     * @return {@code true} if both hold the same values
     */
    @Contract(pure = true)
    boolean matches(@NotNull $Type other);
}
