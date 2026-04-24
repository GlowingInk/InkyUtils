package ink.glowing.params;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

class ParameterImpl {
    private ParameterImpl() { }

    record PlainImpl(@NotNull String rawValue) implements Parameter.Plain {
        static final Parameter.Plain EMPTY = new PlainImpl("");

        @Override
        public int count() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return rawValue.isEmpty();
        }
    }

    record ListedImpl(@NotNull String rawValue, @NotNull List<Parameter> internalValue) implements Parameter.Listed {
        @Override
        public int count() {
            return internalValue.size();
        }

        @Override
        public boolean isEmpty() {
            return internalValue.isEmpty();
        }
    }

    record MappedImpl(@NotNull String rawValue, @ApiStatus.Internal @NotNull Map<String, Parameter> internalValue) implements Parameter.Mapped {
        @Override
        public int count() {
            return internalValue.size();
        }

        @Override
        public boolean isEmpty() {
            return internalValue.isEmpty();
        }
    }
}
