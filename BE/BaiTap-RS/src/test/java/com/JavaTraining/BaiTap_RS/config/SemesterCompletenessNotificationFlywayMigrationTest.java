package com.JavaTraining.BaiTap_RS.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class SemesterCompletenessNotificationFlywayMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    @Test
    void createsNotificationTableAndConstraints() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-notification-table");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_NAME = 'semester_completeness_notification'
                        """)) {
            moveToFirstRow(resultSet, "table query should return a row");
            Assertions.assertEquals(1, resultSet.getInt(1),
                    "V15 should create semester_completeness_notification table");
        }

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                        WHERE CONSTRAINT_NAME IN (
                            'ck_scn_channel_email_only',
                            'ck_scn_status',
                            'uk_sem_notif_chk_recip'
                        )
                        """)) {
            moveToFirstRow(resultSet, "constraint query should return a row");
            Assertions.assertEquals(3, resultSet.getInt(1),
                    "notification migrations should enforce email-only channel, status and unique constraints");
        }

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT CHECK_CLAUSE
                        FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
                        WHERE CONSTRAINT_NAME = 'ck_scn_channel_email_only'
                        """)) {
            moveToFirstRow(resultSet, "email-only channel check should exist");
            String checkClause = resultSet.getString(1);
            Assertions.assertTrue(checkClause.contains("EMAIL"),
                    "channel constraint should allow EMAIL");
            Assertions.assertFalse(checkClause.contains("IN_APP"),
                    "channel constraint should reject IN_APP");
        }
    }

    private void migrate(JdbcDataSource dataSource) {
        Flyway.configure().dataSource(dataSource).locations(MIGRATION_LOCATION).load().migrate();
    }

    private JdbcDataSource dataSource(String databaseName) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(
                "jdbc:h2:mem:"
                        + databaseName
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;"
                        + "NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        return dataSource;
    }

    private void moveToFirstRow(ResultSet resultSet, String errorMessage) throws Exception {
        if (!resultSet.next()) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
