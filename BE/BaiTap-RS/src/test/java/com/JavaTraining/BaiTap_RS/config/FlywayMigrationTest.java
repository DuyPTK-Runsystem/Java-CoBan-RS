package com.JavaTraining.BaiTap_RS.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class FlywayMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    private static final String RAW_PASSWORD = "secret1";

    @Test
    void migratesAnEmptySchemaToTheRoleEnabledBaseline() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-clean");

        Flyway.configure().dataSource(dataSource).locations(MIGRATION_LOCATION).load().migrate();

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM role")) {
            moveToFirstRow(resultSet, "role seed query should return a row");
            Assertions.assertEquals(4, resultSet.getInt(1), "clean migration should seed four roles");
        }
    }

    @Test
    void migratesLegacyUsersToAdministratorsWithoutChangingBcryptHash() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-legacy");
        String bcryptHash = new BCryptPasswordEncoder().encode(RAW_PASSWORD);
        createLegacyUser(dataSource, bcryptHash);

        Flyway.configure()
                .dataSource(dataSource)
                .locations(MIGRATION_LOCATION)
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT app_user.password, role.code
                        FROM app_user
                        JOIN user_role ON user_role.user_id = app_user.user_id
                        JOIN role ON role.role_id = user_role.role_id
                        WHERE app_user.user_name = 'admin01'
                        """)) {
            moveToFirstRow(resultSet, "legacy administrator query should return a row");
            String storedHash = resultSet.getString(1);
            String roleCode = resultSet.getString(2);

            Assertions.assertTrue(
                    bcryptHash.equals(storedHash)
                            && new BCryptPasswordEncoder().matches(RAW_PASSWORD, storedHash)
                            && "ADMIN".equals(roleCode),
                    "legacy migration should preserve the BCrypt hash and grant ADMIN");
        }
    }

    @Test
    void createsAcademicAndEnrollmentSchemaWithRequiredConstraints() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-enrollment");
        Flyway.configure().dataSource(dataSource).locations(MIGRATION_LOCATION).load().migrate();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            verifyEnrollmentSchema(statement);
        }
    }

    @Test
    void createsNullableUniqueStudentUserLink() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-student-user-link");
        Flyway.configure().dataSource(dataSource).locations(MIGRATION_LOCATION).load().migrate();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            boolean nullable;
            int constraintCount;
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT IS_NULLABLE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_NAME = 'student' AND COLUMN_NAME = 'user_id'
                    """)) {
                moveToFirstRow(resultSet, "student user_id column should exist");
                nullable = "YES".equals(resultSet.getString(1));
            }
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                    WHERE TABLE_NAME = 'student'
                      AND CONSTRAINT_NAME IN ('uk_student_user', 'fk_student_user')
                    """)) {
                moveToFirstRow(resultSet, "student user link constraint query should return a row");
                constraintCount = resultSet.getInt(1);
            }
            Assertions.assertTrue(nullable && constraintCount == 2,
                    "student user link must be nullable with unique and foreign key constraints");
        }
    }

    private void verifyEnrollmentSchema(Statement statement) throws Exception {
        checkEnrollmentTables(statement);
        checkEnrollmentUniqueness(statement);
    }

    private void checkEnrollmentTables(Statement statement) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME IN (
                    'grade_level', 'academic_year', 'school_class',
                    'student_year_enrollment', 'class_transfer_history', 'audit_log')
                """)) {
            moveToFirstRow(resultSet, "enrollment table query should return a row");
            Assertions.assertEquals(6, resultSet.getInt(1), "V4 should create all Plan 026 tables");
        }
    }

    private void checkEnrollmentUniqueness(Statement statement) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_NAME = 'uk_enrollment_student_year'
                """)) {
            moveToFirstRow(resultSet, "enrollment constraint query should return a row");
            Assertions.assertEquals(1, resultSet.getInt(1), "student-year uniqueness must be database enforced");
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

    private void createLegacyUser(JdbcDataSource dataSource, String bcryptHash) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE `user` (
                        user_id BIGINT NOT NULL AUTO_INCREMENT,
                        user_name VARCHAR(20) NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        PRIMARY KEY (user_id),
                        CONSTRAINT uk_user_user_name UNIQUE (user_name)
                    )
                    """);
            statement.execute("""
                    INSERT INTO `user` (user_name, password, created_at)
                    VALUES ('admin01', '%s', CURRENT_TIMESTAMP)
                    """.formatted(bcryptHash));
            statement.execute("""
                    CREATE TABLE student (
                        student_id BIGINT NOT NULL AUTO_INCREMENT,
                        student_name VARCHAR(35) NOT NULL,
                        student_code VARCHAR(10) NOT NULL,
                        PRIMARY KEY (student_id),
                        CONSTRAINT uk_student_student_code UNIQUE (student_code)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE student_info (
                        info_id BIGINT NOT NULL AUTO_INCREMENT,
                        student_id BIGINT NOT NULL,
                        address VARCHAR(255) NULL,
                        average_score DOUBLE NULL,
                        date_of_birth DATE NULL,
                        PRIMARY KEY (info_id),
                        CONSTRAINT uk_student_info_student UNIQUE (student_id),
                        CONSTRAINT fk_student_info_student FOREIGN KEY (student_id)
                            REFERENCES student (student_id)
                    )
                    """);
        }
    }

    private void moveToFirstRow(ResultSet resultSet, String errorMessage) throws Exception {
        if (!resultSet.next()) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
