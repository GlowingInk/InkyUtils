package ink.glowing.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Base for single-use builders: elements are added through the child's methods, and the result
 * is produced by {@link #finish()}, which can only be called once.
 * <p>
 * Children must call {@link #checkBuilt()} in every method that modifies the builder.
 * <p>
 * <b>Thread-safety note:</b> Composers are <em>thread-sensitive</em>, meaning instances
 * should not be shared across threads without external synchronization. Each composer
 * can only be used to produce a single result; calling {@link #finish()} more than once
 * will throw an {@link IllegalStateException}.
 * @param <$Result> the type of the built result
 */
public abstract class ComposerBase<$Result> {
    /**
     * Whether {@link #finish()} was already called.
     */
    protected boolean built = false;

    /**
     * Ensures this composer is not finalized yet.
     * @throws IllegalStateException if the composer has already been finalized
     */
    protected final void checkBuilt() {
        if (built) throw new IllegalStateException("This composer is already built");
    }

    /**
     * Finalizes the builder and produces the result. Can only be called once.
     * @return the built result
     * @throws IllegalStateException if the composer has already been finalized
     */
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
