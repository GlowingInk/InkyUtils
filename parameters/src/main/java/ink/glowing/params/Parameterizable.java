package ink.glowing.params;

import org.jetbrains.annotations.NotNull;

public interface Parameterizable {
    @NotNull Parameter asParameter();
}
