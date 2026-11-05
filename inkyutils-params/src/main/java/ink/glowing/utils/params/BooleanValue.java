package ink.glowing.utils.params;

import ink.glowing.utils.bool.TriState;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.BooleanSupplier;

final class BooleanValue extends ValueBase.WithText {
    private static final BooleanValue TRUE = new BooleanValue(TriState.TRUE);
    private static final BooleanValue FALSE = new BooleanValue(TriState.FALSE);
    private static final BooleanValue UNSET = new BooleanValue(TriState.UNSET);

    private final TriState state;

    private BooleanValue(@NotNull TriState state) {
        this(state, state.name().toLowerCase(Locale.ROOT));
    }

    BooleanValue(@NotNull TriState state, @NotNull String text) {
        super(text);
        this.state = state;
    }

    static @NotNull BooleanValue of(@NotNull TriState state) {
        return switch (state) {
            case TRUE -> TRUE;
            case FALSE -> FALSE;
            case UNSET -> UNSET;
        };
    }

    @Override
    public @NotNull TriState asTriState() {
        return state;
    }

    @Override
    public boolean asBoolean(boolean fallback) {
        return state.asBoolean(fallback);
    }

    @Override
    public boolean asBoolean(@NotNull BooleanSupplier fallback) {
        return state.asBoolean(fallback);
    }
}
