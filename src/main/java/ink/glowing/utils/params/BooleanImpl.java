package ink.glowing.utils.params;

import ink.glowing.utils.primitive.TriState;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.BooleanSupplier;

final class BooleanImpl extends TypedImpl {
    private static final BooleanImpl TRUE = new BooleanImpl(TriState.TRUE);
    private static final BooleanImpl FALSE = new BooleanImpl(TriState.FALSE);
    private static final BooleanImpl UNSET = new BooleanImpl(TriState.UNSET);

    private final TriState state;

    private BooleanImpl(@NotNull TriState state) {
        this(state, state.name().toLowerCase(Locale.ROOT));
    }

    BooleanImpl(@NotNull TriState state, @NotNull String text) {
        super(text);
        this.state = state;
    }

    static @NotNull BooleanImpl of(@NotNull TriState state) {
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
    public boolean asBoolean(boolean def) {
        return state.asBoolean(def);
    }

    @Override
    public boolean asBoolean(@NotNull BooleanSupplier def) {
        return state.asBoolean(def);
    }
}
