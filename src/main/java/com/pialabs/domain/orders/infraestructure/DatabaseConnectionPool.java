package com.pialabs.domain.orders.infraestructure;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConnectionPool {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    static {
        String dbUrl = System.getenv("POSTGRES_URL");
        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new IllegalArgumentException("POSTGRES_URL environment variable must be set.");
        }
        config.setJdbcUrl(dbUrl);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        ds = new HikariDataSource(config);
    }

    private DatabaseConnectionPool() {
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static void close() {
        ds.close();
    }
}