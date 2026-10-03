package ink.glowing.utils.params;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;

/**
 * A chain of edits of a list or a map {@link Parameter}, copied once. Get one with
 * {@link Parameter#editList()} or {@link Parameter#editMap()}.
 * <p>
 * Each edit applies to the result of the previous ones, and the edited parameter is never changed.
 * An editor is single-use: any call after {@link #finish()} throws an {@link IllegalStateException}.
 * <p>
 * An absent value ({@code null} or {@link Parameter#missing()}) is never added as an entry.
 */
public sealed interface ParameterEditor permits ParameterEditor.OfList, ParameterEditor.OfMap {
    /**
     * Returns the edited parameter.
     * @return the resulting parameter, or the original if nothing changed
     * @throws IllegalStateException if already finished
     */
    @NotNull @Unmodifiable Parameter finish();

    /**
     * An editor of a list. Indexes are zero-based, and a negative one changes nothing.
     * @see Parameter#editList()
     */
    sealed interface OfList extends ParameterEditor permits ParameterEditorImpl.ListEditor, ParameterEditorImpl.NoopEditor {
        /**
         * Sets the entry at the index. An index past the end extends the list, filling the gap
         * with {@link Parameter#missing()}.
         * @param index the index to set
         * @param value the value, an absent one blanks an existing entry and never extends the list
         * @return this editor
         */
        @NotNull OfList set(int index, @Nullable Parameter value);

        /**
         * Appends a value.
         * @param value the value, skipped if absent
         * @return this editor
         */
        @NotNull OfList add(@Nullable Parameter value);

        /**
         * Inserts a value at the index, shifting the later entries. An index past the end first
         * extends the list with {@link Parameter#missing()}.
         * @param index the index to insert at
         * @param value the value, skipped if absent
         * @return this editor
         */
        @NotNull OfList insert(int index, @Nullable Parameter value);

        /**
         * Inserts the values at the index, in their order, as by {@link #insert(int, Parameter)}.
         * @param index the index to insert at
         * @param values the values, absent ones are skipped
         * @return this editor
         */
        @NotNull OfList insert(int index, @NotNull List<Parameter> values);

        /**
         * Removes the entry at the index, shifting the later ones.
         * @param index the index to remove
         * @return this editor
         */
        @NotNull OfList remove(int index);
    }

    /**
     * An editor of a map.
     * @see Parameter#editMap()
     */
    sealed interface OfMap extends ParameterEditor permits ParameterEditorImpl.MapEditor, ParameterEditorImpl.NoopEditor {
        /**
         * Sets the key, matched ignoring case. An existing key keeps its spelling and position.
         * @param key the key to set
         * @param value the value, an absent one removes the key
         * @return this editor
         */
        @NotNull OfMap put(@NotNull String key, @Nullable Parameter value);

        /**
         * Sets the entries, as by {@link #put(String, Parameter)}.
         * @param entries the entries, {@code null} keys are skipped
         * @return this editor
         */
        @NotNull OfMap putAll(@NotNull Map<String, Parameter> entries);

        /**
         * Removes the key.
         * @param key the key to remove
         * @return this editor
         */
        @NotNull OfMap remove(@NotNull String key);
    }
}
