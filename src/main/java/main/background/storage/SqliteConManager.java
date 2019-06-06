package main.background.storage;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class providing the {@link Connection} to the underlying database.
 * Provides static Methods, which executes {@link ConHandler} or {@link ConConsumer}
 * within a transaction.
 * <p>
 * These methods catch the {@link SQLException}s thrown from the {@code ConHandler} and log
 * them with the package logger.
 * </p>
 * <p>
 * The utility Methods differ only in their return type.
 * </p>
 */
final class SqliteConManager {
    private static Logger logger = Register.LOGGER;
    private final String database;
    private final HikariDataSource hikariSource;

    SqliteConManager(String name, String location) {
        this.database = "jdbc:sqlite:" + location + "\\" + name + ".db";
        this.hikariSource = new HikariDataSource();
        this.hikariSource.setJdbcUrl(database);
//        this.hikariSource.setDataSourceClassName("org.sqlite.SQLiteDataSource");
    }

    /**
     * @param connectionRFunction
     * @param <E>
     * @return
     */
    <E> E getConnectionAuto(ConHandler<Connection, E> connectionRFunction) {
        E result = null;
        try (Connection connection = connection()) {
            try {
                connection.setAutoCommit(true);
                result = connectionRFunction.handle(connection);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        } catch (SQLException e) {
            logConnectionError(e);
        }
        return result;
    }

    /**
     * Gets the connection of the underlying database.
     *
     * @return connection a {@code Connection}
     * @throws SQLException if DriverManager could not getGorgon connection
     */
    Connection connection() throws SQLException {
        System.out.println(String.format("connecting on %s", Thread.currentThread().getName()));
        return this.hikariSource.getConnection();
//        return DriverManager.getConnection(this.database);
//        if (connection == null || connection.isClosed()) {
//            connection = DriverManager.getConnection("jdbc:sqlite:" + database);
//        }
//        return connection;
    }

    /**
     * Logs the thrown Exception in case of connection error.
     *
     * @param e a {@code SQLException}, which wil be logged
     */
    private void logConnectionError(SQLException e) {
        logger.log(Level.SEVERE, "error in establishing the connection", e);
    }

    /**
     * @param connectionRFunction
     */
    void getCon(ConConsumer<Connection> connectionRFunction) {
        try {
            try (Connection connection = connection()) {
                try {
                    connection.setAutoCommit(false);
                    connectionRFunction.handle(connection);
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (RuntimeException e) {
                    connection.rollback();
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            logConnectionError(e);
        }
    }

    /**
     * @param connectionRFunction
     */
    void getConAuto(ConConsumer<Connection> connectionRFunction) {
        try (Connection connection = connection()) {
            connection.setAutoCommit(true);
            connectionRFunction.handle(connection);
        } catch (SQLException e) {
            logConnectionError(e);
        }
    }

    /**
     * @param connectionRFunction
     * @param <E>
     * @return
     */
    <E> E getConnection(ConHandler<Connection, E> connectionRFunction) {
        E result = null;
        try {
            try (Connection connection = connection()) {
                try {
                    connection.setAutoCommit(false);
                    result = connectionRFunction.handle(connection);
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (RuntimeException e) {
                    connection.rollback();
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            logConnectionError(e);
        }
        return result;
    }
}
