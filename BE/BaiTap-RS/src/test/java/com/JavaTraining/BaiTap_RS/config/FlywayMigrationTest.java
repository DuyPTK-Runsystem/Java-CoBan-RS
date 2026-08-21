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

    private static final String RAW_PASSWORD = "secret1";

    @Test
    void migratesAnEmptySchemaToTheRoleEnabledBaseline() throws Exception {
        JdbcDataSource dataSource = dataSource("flyway-clean");

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

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
                .locations("classpath:db/migration")
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
        }
    }

    private void moveToFirstRow(ResultSet resultSet, String errorMessage) throws Exception {
        if (!resultSet.next()) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
