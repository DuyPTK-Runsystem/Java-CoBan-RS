package com.JavaTraining.BaiTap_RS.notification;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NotificationMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    @Test
    void v26EnforcesNotificationV3ConstraintsAfterV25() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-notification-school-scope");
        Flyway.configure().dataSource(dataSource).locations(MIGRATION_LOCATION).load().migrate();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT checksum
                    FROM flyway_schema_history
                    WHERE version = '25'
                    """)) {
                Assertions.assertTrue(resultSet.next());
                Assertions.assertEquals(-164191837, resultSet.getInt(1));
            }

            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                    WHERE TABLE_NAME = 'notification'
                      AND CONSTRAINT_NAME = 'ck_notification_channel'
                    """)) {
                Assertions.assertTrue(resultSet.next());
                Assertions.assertEquals(1, resultSet.getInt(1));
            }

            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT CHECK_CLAUSE
                    FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
                    WHERE CONSTRAINT_NAME = 'ck_notification_channel'
                    """)) {
                Assertions.assertTrue(resultSet.next());
                String checkClause = resultSet.getString(1);
                Assertions.assertTrue(checkClause.contains("IN_APP"));
                Assertions.assertFalse(checkClause.contains("EMAIL"));
            }

            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                    WHERE TABLE_NAME = 'notification'
                      AND CONSTRAINT_NAME = 'ck_notification_school_scope'
                    """)) {
                Assertions.assertTrue(resultSet.next());
                Assertions.assertEquals(1, resultSet.getInt(1));
            }

            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_NAME = 'notification'
                      AND COLUMN_NAME = 'idempotency_fingerprint'
                    """)) {
                Assertions.assertTrue(resultSet.next());
                Assertions.assertEquals(1, resultSet.getInt(1));
            }

            Assertions.assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO notification (
                        title, body, channel, status, audience_type, school_scope, sender_id)
                    VALUES ('Ngoài scope', 'Không hợp lệ', 'IN_APP', 'DRAFT', 'SCHOOL', 'OTHER_SCHOOL', 1)
                    """));
        }
    }

    private JdbcDataSource dataSource(String databaseName) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(
                "jdbc:h2:mem:"
                        + databaseName
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        return dataSource;
    }
}
