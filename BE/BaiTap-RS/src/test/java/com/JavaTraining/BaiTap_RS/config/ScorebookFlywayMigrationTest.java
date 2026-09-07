package com.JavaTraining.BaiTap_RS.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ScorebookFlywayMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    @Test
    void createsScorebookFoundationTables() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-scorebook-tables");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_NAME IN (
                            'scorebook', 'assessment_column', 'skill_weight_config',
                            'student_score', 'student_annual_transcript',
                            'student_term_transcript', 'calculation_task',
                            'student_subject_term_result', 'student_subject_annual_result')
                        """)) {
            moveToFirstRow(resultSet, "scorebook table query should return a row");
            Assertions.assertEquals(9, resultSet.getInt(1), "V10, V11, V12 and V16 should create all scorebook tables");
        }
    }

    @Test
    void createsScorebookFoundationConstraints() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-scorebook-constraints");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                        WHERE CONSTRAINT_NAME IN (
                            'uk_scorebook_class_subject',
                            'uk_assessment_column_position',
                            'uk_skill_weight_scorebook',
                            'ck_skill_weight_total',
                            'uk_student_score_column_student',
                            'uk_annual_transcript_student_year',
                            'uk_term_transcript_annual_semester',
                            'uk_calculation_task_idempotency',
                            'uk_subject_term_result',
                            'uk_subject_annual_result')
                        """)) {
            moveToFirstRow(resultSet, "scorebook constraint query should return a row");
            Assertions.assertEquals(
                    10,
                    resultSet.getInt(1),
                    "V10, V11, V12 and V16 should enforce scorebook uniqueness constraints");
        }
    }

    @Test
    void createsTranscriptResultColumnsIndexesAndForeignKeys() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-transcript-result-schema");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE (TABLE_NAME = 'student_annual_transcript'
                                AND COLUMN_NAME IN ('regular_dtbcn', 'final_dtbcn', 'result_source',
                                        'last_calculation_task_id'))
                           OR (TABLE_NAME = 'student_term_transcript' AND COLUMN_NAME = 'dtbhk')
                        """)) {
            moveToFirstRow(resultSet, "transcript result column query should return a row");
            Assertions.assertEquals(5, resultSet.getInt(1), "V16 should add all transcript result columns");
        }

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                        WHERE CONSTRAINT_NAME IN (
                            'ck_annual_transcript_result_source',
                            'ck_annual_transcript_regular_dtbcn',
                            'ck_annual_transcript_final_dtbcn',
                            'ck_term_transcript_dtbhk',
                            'fk_annual_transcript_calc_task',
                            'fk_subject_term_result_transcript',
                            'fk_subject_term_result_class_subject',
                            'fk_subject_term_result_subject',
                            'fk_subject_annual_result_transcript',
                            'fk_subject_annual_result_subject',
                            'fk_subject_annual_result_hk1',
                            'fk_subject_annual_result_hk2')
                        """)) {
            moveToFirstRow(resultSet, "transcript result constraint query should return a row");
            Assertions.assertEquals(
                    12,
                    resultSet.getInt(1),
                    "V16 should create all result constraints and foreign keys");
        }

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.INDEXES
                        WHERE INDEX_NAME IN (
                            'idx_subject_term_result_transcript',
                            'idx_subject_term_result_class_subject',
                            'idx_subject_annual_result_transcript',
                            'idx_subject_annual_result_subject')
                        """)) {
            moveToFirstRow(resultSet, "transcript result index query should return a row");
            Assertions.assertEquals(4, resultSet.getInt(1), "V16 should create all result lookup indexes");
        }
    }

    @Test
    void createsRetakeExamTableConstraintsAndIndexes() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-retake-exam-schema");
        migrate(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_NAME = 'retake_exam'
                        """)) {
            moveToFirstRow(resultSet, "retake_exam table query should return a row");
            Assertions.assertEquals(1, resultSet.getInt(1), "V17 should create retake_exam table");
        }

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                        WHERE CONSTRAINT_NAME IN (
                            'uk_retake_student_year_subject',
                            'ck_retake_exam_status',
                            'ck_retake_pre_score',
                            'ck_retake_score',
                            'fk_retake_exam_student',
                            'fk_retake_exam_academic_year',
                            'fk_retake_exam_subject',
                            'fk_subject_annual_result_retake')
                        """)) {
            moveToFirstRow(resultSet, "retake_exam constraint query should return a row");
            Assertions.assertEquals(8, resultSet.getInt(1),
                    "V17 should create all retake constraints and foreign keys");
        }

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.INDEXES
                        WHERE INDEX_NAME IN (
                            'idx_retake_exam_student_year',
                            'idx_retake_exam_status')
                        """)) {
            moveToFirstRow(resultSet, "retake_exam index query should return a row");
            Assertions.assertEquals(2, resultSet.getInt(1), "V17 should create retake indexes");
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
