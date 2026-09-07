package com.JavaTraining.BaiTap_RS.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class SemesterLockFlywayMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    @Test
    void createsSemesterLockTablesAndColumns() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-semester-lock-tables");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_NAME IN ('semester_lock_run', 'semester_lock_report')
                        """)) {
            moveToFirstRow(resultSet, "table query should return a row");
            Assertions.assertEquals(2, resultSet.getInt(1), "V14 should create semester_lock_run and report tables");
        }

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_NAME = 'semester' AND COLUMN_NAME = 'reopen_until'
                        """)) {
            moveToFirstRow(resultSet, "column query should return a row");
            Assertions.assertEquals(1, resultSet.getInt(1), "V14 should add reopen_until to semester");
        }
    }

    @Test
    void createsSemesterLockConstraints() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-semester-lock-constraints");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                        WHERE CONSTRAINT_NAME IN (
                            'ck_semester_lock_run_status',
                            'ck_semester_lock_report_status',
                            'uk_lock_report_run_sem_chk'
                        )
                        """)) {
            moveToFirstRow(resultSet, "constraint query should return a row");
            Assertions.assertEquals(3, resultSet.getInt(1),
                    "V14 should enforce run/report status and unique constraints");
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
