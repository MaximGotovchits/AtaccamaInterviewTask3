package com.project.ataccama3.util;

import com.project.ataccama3.model.DBConnection;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConnectionManager {
    private final Map<DBConnection, JdbcTemplate> connectionToDataSource = new ConcurrentHashMap<>();
    private final ReadWriteLock clearCacheLock = new ReentrantReadWriteLock();

    private static final Integer CONNECTION_LIMIT = 5;

    public JdbcTemplate getJdbcTemplate(DBConnection dbConnection) throws Exception {
//        Can be modified in future to cache not more than CONNECTION_LIMIT of connections using heap-like data structure (e.g. ConcurrentSkipListMap).
        if (connectionToDataSource.size() > CONNECTION_LIMIT) {
            try {
                clearCacheLock.writeLock().lock(); // Only 1 thread can clean the cache.
                Set<DBConnection> dbConnectionSet = connectionToDataSource.keySet();
                for (DBConnection connectionToRemove : dbConnectionSet) {
                    DataSource dataSourceToClose = connectionToDataSource.get(connectionToRemove).getDataSource();
                    if (dataSourceToClose != null) {
                        ((HikariDataSource) dataSourceToClose).close();
                    }
                    connectionToDataSource.remove(connectionToRemove);
                    if (connectionToDataSource.size() <= CONNECTION_LIMIT / 2) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new Exception("Failed to clean connection cache: " + e.getMessage());
            } finally {
                clearCacheLock.writeLock().unlock();
            }
        }
        try {
            clearCacheLock.readLock().lock();
            return connectionToDataSource.computeIfAbsent(dbConnection, conn -> new JdbcTemplate(initDataSource(dbConnection)));
        } catch (Exception e) {
            throw new Exception("Failed to get a connection: " + e.getMessage());
        } finally {
            clearCacheLock.readLock().unlock();
        }
    }

    private HikariDataSource initDataSource(DBConnection dbConnection) {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .type(HikariDataSource.class)
                .url(constructUrl(dbConnection))
                .username(dbConnection.getUser())
                .password(dbConnection.getPassword())
                .build();
    }

    private String constructUrl(DBConnection dbConnection) {
        return dbConnection.getDbPrefix() + dbConnection.getHost() + ":" + dbConnection.getPort() + "/" + dbConnection.getSchema();
    }
}
