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
     * @param def the fallback, which also gives the enum
     * @return the matching constant, or the fallback if there is none
     */
    @Contract(pure = true)
    public static <$Enum extends Enum<$Enum>> @NotNull $Enum asEnum(@NotNull String name, @NotNull $Enum def) {
        return asEnum(name, def.getDeclaringClass(), def);
    }

    /**
     * Finds the constant of the enum by its name, ignoring case.
     * @param <$Enum> the type of the enum
     * @param name the name to look up
     * @param type the enum class
     * @return the matching constant, or empty if there is none
     */
    @Contract(pure = true)
    public static <$Enum extends Enum<$Enum>> @NotNull Optional<$Enum> asEnum(@NotNull String name, @NotNull Class<$Enum> type) {
        return Optional.ofNullable(asEnum(name, type, null));
    }

    /**
     * Finds the constant of the enum by its name, ignoring case.
     * @param <$Enum> the type of the enum
     * @param name the name to look up
     * @param type the enum class
     * @param def the fallback
     * @return the matching constant, or the fallback if there is none
     * @see Enum#valueOf(Class, String)
     */
    @Contract(value = "_, _, !null -> !null", pure = true)
    public static <$Enum extends Enum<$Enum>> @Nullable $Enum asEnum(@NotNull String name, @NotNull Class<$Enum> type, @Nullable $Enum def) {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            for ($Enum constant : type.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(name)) return constant;
            }
            return def;
        }
    }
}
