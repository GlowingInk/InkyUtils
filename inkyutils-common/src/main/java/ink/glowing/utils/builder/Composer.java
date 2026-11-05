package ink.glowing.utils.builder;

import org.jetbrains.annotations.Contract;

/**
 * Single-use builders: elements are added through the child's methods, and the result
 * is produced by {@link #finish()}, which can only be called once.
 * <p>
 * <b>Thread-safety note:</b> Composers are <em>thread-sensitive</em>, meaning instances
 * should not be shared across threads without external synchronization. Each composer
 * can only be used to produce a single result; calling {@link #finish()} more than once
 * will throw an {@link IllegalStateException}.
 * @param <$Result> the type of the built result
 */
public interface Composer<$Result> {
    /**
     * Returns whether {@link #finish()} was already called.
     * @return {@code true} if this composer is finalized
     */
    @Contract(pure = true)
    boolean isBuilt();

    /**
     * Finalizes the builder and produces the result. Can only be called once.
     * @return the built result
     * @throws IllegalStateException if the composer has already been finalized
     */
    $Result finish();
}
