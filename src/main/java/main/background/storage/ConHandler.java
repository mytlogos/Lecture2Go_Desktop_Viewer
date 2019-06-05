package main.background.storage;

import java.sql.SQLException;

/**
 *
 */
@FunctionalInterface
public interface ConHandler<T, R> {
    R handle(T t) throws SQLException;
}

