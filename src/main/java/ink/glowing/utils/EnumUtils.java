package ink.glowing.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Helpers for working with enums.
 */
public final class EnumUtils {
    private EnumUtils() { }

    /**
     * Finds the constant of the fallback's enum by its name, ignoring case.
     * @param <$Enum> the type of the enum
     * @param name the name to look up
     * @param fallback the fallback, which also gives the enum
     * @return the matching constant, or the fallback if there is none
     */
    @Contract(pure = true)
    public static <$Enum extends Enum<$Enum>> @NotNull $Enum parseEnum(@NotNull String name, @NotNull $Enum fallback) {
        return parseEnum(name, fallback.getDeclaringClass(), fallback);
    }

    /**
     * Finds the constant of the enum by its name, ignoring case.
     * @param <$Enum> the type of the enum
     * @param name the name to look up
     * @param type the enum class
     * @return the matching constant, or empty if there is none
     */
    @Contract(pure = true)
    public static <$Enum extends Enum<$Enum>> @NotNull Optional<$Enum> parseEnum(@NotNull String name, @NotNull Class<$Enum> type) {
        return Optional.ofNullable(parseEnum(name, type, null));
    }

    /**
     * Finds the constant of the enum by its name, ignoring case.
     * @param <$Enum> the type of the enum
     * @param name the name to look up
     * @param type the enum class
     * @param fallback the fallback
     * @return the matching constant, or the fallback if there is none
     * @see Enum#valueOf(Class, String)
     */
    @Contract(value = "_, _, !null -> !null", pure = true)
    public static <$Enum extends Enum<$Enum>> @Nullable $Enum parseEnum(@NotNull String name, @NotNull Class<$Enum> type, @Nullable $Enum fallback) {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            for ($Enum constant : type.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(name)) return constant;
            }
            return fallback;
        }
    }
}
