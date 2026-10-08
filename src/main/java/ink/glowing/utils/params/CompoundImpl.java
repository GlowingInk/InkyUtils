package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Shared base of the parameters that hold nested parameters: their fields, equality and the
 * serialization skeleton, which only differs in the brackets and how the entries are appended.
 * <p>
 * The serialized forms are cached. The cache is intentionally racy: threads that miss it at
 * the same time may compute the value more than once, but they all compute the same string,
 * and {@code String} is safely published through a data race.
 */
abstract sealed class CompoundImpl implements Parameter permits ListedImpl, MappedImpl {
    private final Function<Parameter, String> rawCompute;

    private String topLevelForm;
    private String nestedForm;
    private String unescapedRaw;

    CompoundImpl(@NotNull Function<Parameter, String> rawCompute) {
        this.rawCompute = rawCompute;
    }

    abstract char open();

    abstract char close();

    /**
     * Appends every entry followed by a space.
     */
    abstract void appendEntries(@NotNull StringBuilder sb);

    @Override
    public final @NotNull String raw() {
        return rawCompute.apply(this);
    }

    @Override
    public final @NotNull String textValue() {
        String cached = unescapedRaw;
        if (cached == null) {
            unescapedRaw = cached = Parameter.unescape(raw());
        }
        return cached;
    }

    @Override
    public final @NotNull String serialize(boolean topLevel) {
        String cached = topLevel ? topLevelForm : nestedForm;
        if (cached == null) {
            cached = computeSerialized(topLevel);
            if (topLevel) {
                topLevelForm = cached;
            } else {
                nestedForm = cached;
            }
        }
        return cached;
    }

    private @NotNull String computeSerialized(boolean topLevel) {
        if (count() == 0) {
            return topLevel ? "" : "" + open() + close();
        }
        StringBuilder sb = new StringBuilder();
        if (!topLevel) sb.append(open());
        appendEntries(sb);
        if (topLevel) {
            sb.setLength(sb.length() - 1);
        } else {
            sb.setCharAt(sb.length() - 1, close());
        }
        return sb.toString();
    }

    @Override
    public final boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass() == getClass() && raw().equals(((Parameter) obj).raw());
    }

    @Override
    public final int hashCode() {
        return raw().hashCode();
    }

    @Override
    public final String toString() {
        return textValue();
    }
}
