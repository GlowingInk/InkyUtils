package ink.glowing.utils.builder;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A base abstract class for {@link Composer} implementations.
 * Children must call {@link #checkBuilt()} in every method that modifies the builder.
 */
public abstract class ComposerBase<$Result> implements Composer<$Result> {
    private boolean built = false;

    /**
     * Creates a composer.
     */
    protected ComposerBase() { }

    @Contract(pure = true)
    @Override
    public final boolean isBuilt() {
        return built;
    }

    /**
     * Ensures this composer is not finalized yet.
     * @throws IllegalStateException if the composer has already been finalized
     */
    protected final void checkBuilt() {
        if (built) throw new IllegalStateException("This composer is already built");
    }

    @Override
    public final @NotNull $Result finish() {
        checkBuilt();
        built = true;
        return doFinish();
    }

    /**
     * Produces the result. Called by {@link #finish()}, after it has checked and marked
     * this composer as built.
     * @return the built result
     */
    protected abstract @NotNull $Result doFinish();
}
