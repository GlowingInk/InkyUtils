package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

final class ParameterHelper {
    private ParameterHelper() { }

    static final Function<Parameter, String> EMPTY_RAW = _ -> "";

    static final Function<Parameter, String> SERIALIZED_RAW = param -> param.serialize(true);

    static final Function<Parameter, String> VALUE_RAW = Parameter::textValue;

    static @NotNull Parameter getByIndexKey(@NotNull Parameter self, @NotNull String key) {
        int length = key.length();
        if (length == 0) return MissingImpl.INSTANCE;
        for (int i = 0; i < length; i++) {
            char ch = key.charAt(i);
            boolean sign = i == 0 && length > 1 && (ch == '-' || ch == '+');
            if (!sign && Character.digit(ch, 10) < 0) return MissingImpl.INSTANCE;
        }
        try {
            return self.get(Integer.parseInt(key));
        } catch (NumberFormatException _) { // out of the int range
            return MissingImpl.INSTANCE;
        }
    }

    static boolean isAbsent(@Nullable Parameter parameter) {
        return parameter == null || parameter == MissingImpl.INSTANCE;
    }

    static @NotNull Parameter orMissing(@Nullable Parameter parameter) {
        return parameter == null ? MissingImpl.INSTANCE : parameter;
    }

    static @NotNull String serializeEntry(@NotNull Parameter entry) {
        return entry == MissingImpl.INSTANCE ? "''" : entry.serialize(false);
    }
}
