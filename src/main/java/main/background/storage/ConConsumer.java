package main.background.storage;

import java.sql.SQLException;

/**
 *
 */
@FunctionalInterface
public interface ConConsumer<T> {
    void handle(T t) throws SQLException;
}
