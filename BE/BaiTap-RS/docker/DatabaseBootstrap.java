import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseBootstrap {

    private static final int MAX_ATTEMPTS = 30;
    private static final long RETRY_DELAY_MILLIS = 2000L;

    private DatabaseBootstrap() {
    }

    public static void main(String[] args) {
        String host = getRequiredEnvironment("DATABASE_HOST");
        String port = getRequiredEnvironment("DATABASE_PORT");
        String databaseName = getRequiredEnvironment("DATABASE_NAME");
        String username = getRequiredEnvironment("DATABASE_BOOTSTRAP_USER");
        String password = getRequiredEnvironment("DATABASE_BOOTSTRAP_PASSWORD");

        validatePort(port);
        validateDatabaseName(databaseName);

        String jdbcUrl = "jdbc:mysql://" + host + ':' + port + '/';
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                    Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + databaseName + "`");
                System.out.println("Database " + databaseName + " is ready.");
                return;
            } catch (SQLException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    System.err.println("Could not prepare database " + databaseName + " after "
                            + MAX_ATTEMPTS + " attempts: " + exception.getMessage());
                    System.exit(1);
                }

                System.err.println("Waiting for MySQL at " + host + ':' + port + " (" + attempt + '/' + MAX_ATTEMPTS
                        + ")...");
                sleepBeforeRetry();
            }
        }
    }

    private static String getRequiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be set.");
        }
        return value;
    }

    private static void validatePort(String port) {
        try {
            int numericPort = Integer.parseInt(port);
            if (numericPort < 1 || numericPort > 65535) {
                throw new IllegalArgumentException("DATABASE_PORT must be between 1 and 65535.");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("DATABASE_PORT must be numeric.", exception);
        }
    }

    private static void validateDatabaseName(String databaseName) {
        if (!databaseName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "DATABASE_NAME must contain only letters, numbers, or underscores.");
        }
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for MySQL.", exception);
        }
    }
}
