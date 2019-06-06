package main;

import java.util.*;

/**
 *
 */
public class QueueLikeMap<K, V> implements Map<K, V> {
    private final int limit;
    private LinkedHashMap<K, V> map = new LinkedHashMap<>();

    public QueueLikeMap(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        if (this.map.size() >= this.limit) {
            final Iterator<K> iterator = this.map.keySet().iterator();

            while (iterator.hasNext() && this.map.size() >= this.limit) {
                iterator.next();
                iterator.remove();
            }
        }
        return this.map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return this.map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        int toRemove = (this.size() + m.size()) - this.limit;

        if (toRemove > 0) {
            final Iterator<K> iterator = this.map.keySet().iterator();

            while (iterator.hasNext() && toRemove > 0) {
                iterator.next();
                iterator.remove();
                toRemove--;
            }
        }
        this.map.putAll(m);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set<K> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<V> values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }
}
