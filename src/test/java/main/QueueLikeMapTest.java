package main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
class QueueLikeMapTest {

    private QueueLikeMap<String, String> map = new QueueLikeMap<>(5);
    private LinkedHashMap<String, String> data = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        this.map.clear();
        final int limit = this.map.getLimit() + 10;

        for (int i = 0; i < limit; i++) {
            this.data.put("" + i, "hello" + i);
        }
    }

    @Test
    void put() {
        for (Map.Entry<String, String> entry : this.data.entrySet()) {
            this.map.put(entry.getKey(), entry.getValue());
            Assertions.assertTrue(this.map.getLimit() >= this.map.size());
            Assertions.assertTrue(this.map.containsKey(entry.getKey()));
            Assertions.assertTrue(this.map.containsValue(entry.getValue()));
        }
        final List<String> strings = new ArrayList<>(this.map.keySet());
        final List<String> entries = new ArrayList<>(this.data.keySet());
        final int moreEntries = entries.size() - strings.size();

        for (int i = 0; i < entries.size(); i++) {
            if (i < moreEntries) {
                Assertions.assertFalse(this.map.containsKey(entries.get(i)));
            } else {
                Assertions.assertEquals(entries.get(i), strings.get(i - moreEntries));
            }
        }
    }

    @Test
    void putAll() {
        this.map.putAll(this.data);

        final List<String> strings = new ArrayList<>(this.map.keySet());
        final List<String> entries = new ArrayList<>(this.data.keySet());
        final int moreEntries = entries.size() - strings.size();

        for (int i = 0; i < entries.size(); i++) {
            if (i < moreEntries) {
                Assertions.assertFalse(this.map.containsKey(entries.get(i)));
            } else {
                Assertions.assertEquals(entries.get(i), strings.get(i - moreEntries));
            }
        }
    }
}