package com.JavaTraining.BaiTap_RS.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ScorebookFlywayMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    @Test
    void createsScorebookFoundationTables() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-scorebook-tables");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_NAME IN ('scorebook', 'assessment_column', 'skill_weight_config')
                        """)) {
            moveToFirstRow(resultSet, "scorebook table query should return a row");
            Assertions.assertEquals(3, resultSet.getInt(1), "V10 should create all scorebook foundation tables");
        }
    }

    @Test
    void createsScorebookFoundationConstraints() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-scorebook-constraints");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                        WHERE CONSTRAINT_NAME IN (
                            'uk_scorebook_class_subject',
                            'uk_assessment_column_position',
                            'uk_skill_weight_scorebook',
                            'ck_skill_weight_total')
                        """)) {
            moveToFirstRow(resultSet, "scorebook constraint query should return a row");
            Assertions.assertEquals(
                    4,
                    resultSet.getInt(1),
                    "V10 should enforce scorebook uniqueness and weight constraints");
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
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        return dataSource;
    }

    private void moveToFirstRow(ResultSet resultSet, String errorMessage) throws Exception {
        if (!resultSet.next()) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
