package main.background.storage;

import main.model.QueryItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 *
 */
public abstract class QueryItemHelper<T extends QueryItem> {
    final String key;
    final BiFunction<Integer, String, T> createFunction;

    // todo implement this
    public QueryItemHelper(String key, BiFunction<Integer, String, T> createFunction) {
        this.key = key;
        this.createFunction = createFunction;
    }

    public void add(Collection<T> items) {
        for (T item : items) {
            this.add(item);
        }
    }

    public abstract void add(T item);

    public void delete(Collection<T> items) {
        for (T item : items) {
            this.delete(item);
        }
    }

    public abstract void delete(T item);

    public abstract List<T> getAll();
}
