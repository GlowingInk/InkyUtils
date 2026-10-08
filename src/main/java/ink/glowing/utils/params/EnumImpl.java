package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

final class EnumImpl extends TypedImpl {
    private final Enum<?> constant;

    EnumImpl(@NotNull Enum<?> constant) {
        super(constant.name());
        this.constant = constant;
    }

    @Override
    public <$Enum extends Enum<$Enum>> @NotNull Optional<$Enum> asEnum(@NotNull Class<$Enum> type) {
        return type.isInstance(constant) ? Optional.of(type.cast(constant)) : super.asEnum(type);
    }

    @Override
    public <$Enum extends Enum<$Enum>> @Nullable $Enum asEnum(@NotNull Class<$Enum> type, @Nullable $Enum def) {
        return type.isInstance(constant) ? type.cast(constant) : super.asEnum(type, def);
    }

    @Override
    public <$Enum extends Enum<$Enum>> @NotNull $Enum asEnum(@NotNull $Enum def) {
        Class<$Enum> type = def.getDeclaringClass();
        return type.isInstance(constant) ? type.cast(constant) : super.asEnum(def);
    }
}
