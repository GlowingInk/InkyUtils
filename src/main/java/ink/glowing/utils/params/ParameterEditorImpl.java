package ink.glowing.utils.params;

import ink.glowing.utils.ComposerBase;
import ink.glowing.utils.params.ParameterImpl.ListedImpl;
import ink.glowing.utils.params.ParameterImpl.MappedImpl;
import ink.glowing.utils.params.ParameterImpl.MissingImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ParameterEditorImpl {
    private ParameterEditorImpl() { }

    static final class ListEditor extends ComposerBase<Parameter> implements ParameterEditor.OfList {
        private final ListedImpl source;
        private ArrayList<Parameter> list;

        ListEditor(@NotNull ListedImpl source) {
            this.source = source;
        }

        private ArrayList<Parameter> working() {
            if (list == null) list = new ArrayList<>(source.internalValue);
            return list;
        }

        private int size() {
            return list == null ? source.internalValue.size() : list.size();
        }

        private void fillTo(ArrayList<Parameter> target, int size) {
            while (target.size() < size) {
                target.add(MissingImpl.INSTANCE);
            }
        }

        @Override
        public @NotNull ListEditor set(int index, @Nullable Parameter value) {
            checkBuilt();
            if (index < 0 || index >= size() && ParameterImpl.isAbsent(value)) return this;

            ArrayList<Parameter> target = working();
            fillTo(target, index + 1);
            target.set(index, ParameterImpl.orMissing(value));
            return this;
        }

        @Override
        public @NotNull ListEditor add(@Nullable Parameter value) {
            checkBuilt();
            if (!ParameterImpl.isAbsent(value)) working().add(value);
            return this;
        }

        @Override
        public @NotNull ListEditor insert(int index, @Nullable Parameter value) {
            checkBuilt();
            if (index < 0 || ParameterImpl.isAbsent(value)) return this;

            ArrayList<Parameter> target = working();
            fillTo(target, index);
            target.add(index, value);
            return this;
        }

        @Override
        public @NotNull ListEditor insert(int index, @NotNull List<Parameter> values) {
            checkBuilt();
            if (index < 0) return this;

            ArrayList<Parameter> present = new ArrayList<>(values.size());
            for (Parameter value : values) {
                if (!ParameterImpl.isAbsent(value)) present.add(value);
            }
            if (present.isEmpty()) return this;

            ArrayList<Parameter> target = working();
            fillTo(target, index);
            target.addAll(index, present);
            return this;
        }

        @Override
        public @NotNull ListEditor remove(int index) {
            checkBuilt();
            if (index >= 0 && index < size()) working().remove(index);
            return this;
        }

        @Override
        protected @NotNull Parameter doFinish() {
            if (list == null) return source;

            list.trimToSize();
            return new ListedImpl(ParameterImpl.SERIALIZED_RAW, list);
        }
    }

    static final class MapEditor extends ComposerBase<Parameter> implements ParameterEditor.OfMap {
        private final MappedImpl source;
        private Object2ObjectLinkedOpenCustomHashMap<String, Parameter> map;

        MapEditor(@NotNull MappedImpl source) {
            this.source = source;
        }

        private Object2ObjectLinkedOpenCustomHashMap<String, Parameter> working() {
            if (map == null) map = source.internalValue.clone();
            return map;
        }

        @Override
        public @NotNull MapEditor put(@NotNull String key, @Nullable Parameter value) {
            checkBuilt();
            if (ParameterImpl.isAbsent(value)) return remove(key);

            working().put(key, value);
            return this;
        }

        @Override
        public @NotNull MapEditor putAll(@NotNull Map<String, Parameter> entries) {
            checkBuilt();
            for (Map.Entry<String, Parameter> entry : entries.entrySet()) {
                if (entry.getKey() != null) put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        @Override
        public @NotNull MapEditor remove(@NotNull String key) {
            checkBuilt();
            if ((map == null ? source.internalValue : map).containsKey(key)) working().remove(key);
            return this;
        }

        @Override
        protected @NotNull Parameter doFinish() {
            return map == null ? source : new MappedImpl(ParameterImpl.SERIALIZED_RAW, map);
        }
    }

    static final class NoopEditor extends ComposerBase<Parameter> implements ParameterEditor.OfList, ParameterEditor.OfMap {
        private final Parameter source;

        NoopEditor(@NotNull Parameter source) {
            this.source = source;
        }

        @Override
        public @NotNull NoopEditor set(int index, @Nullable Parameter value) {
            checkBuilt();
            return this;
        }

        @Override
        public @NotNull NoopEditor add(@Nullable Parameter value) {
            checkBuilt();
            return this;
        }

        @Override
        public @NotNull NoopEditor insert(int index, @Nullable Parameter value) {
            checkBuilt();
            return this;
        }

        @Override
        public @NotNull NoopEditor insert(int index, @NotNull List<Parameter> values) {
            checkBuilt();
            return this;
        }

        @Override
        public @NotNull NoopEditor remove(int index) {
            checkBuilt();
            return this;
        }

        @Override
        public @NotNull NoopEditor put(@NotNull String key, @Nullable Parameter value) {
            checkBuilt();
            return this;
        }

        @Override
        public @NotNull NoopEditor putAll(@NotNull Map<String, Parameter> entries) {
            checkBuilt();
            return this;
        }

        @Override
        public @NotNull NoopEditor remove(@NotNull String key) {
            checkBuilt();
            return this;
        }

        @Override
        protected @NotNull Parameter doFinish() {
            return source;
        }
    }
}
