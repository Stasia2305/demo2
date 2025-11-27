// java
package dk.easv.mytunes.dal;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private final SQLServerDataSource ds;
    private final String jdbcUrl;
    private final String user;
    private final String password;

    public DatabaseConnection() throws SQLServerException {
        this.ds = new SQLServerDataSource();

        this.jdbcUrl = getenv("JDBC_URL", "").trim();

        if (!jdbcUrl.isEmpty()) {
            // full URL provided
            ds.setURL(jdbcUrl);
            this.user = getenv("DB_USER", "");
            this.password = getenv("DB_PASSWORD", "");
        } else {
            // per-field configuration with sensible defaults
            ds.setDatabaseName(getenv("DB_NAME", "natbur001_MyTunes"));
            ds.setUser(getenv("DB_USER", "CS2025b_e_4"));
            ds.setPassword(getenv("DB_PASSWORD", "CS2025bE4#23"));
            ds.setServerName(getenv("DB_SERVER", "EASV-DB4"));

            int port = 1433;
            try {
                port = Integer.parseInt(getenv("DB_PORT", "1433"));
            } catch (NumberFormatException ignored) { }
            ds.setPortNumber(port);
            this.user = ds.getUser();
            this.password = String.valueOf(ds.getConnection());

        }
    }

    private String getenv(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    /**
     * Get a live connection. First tries the configured SQLServerDataSource;
     * if that fails and a JDBC_URL was provided, tries DriverManager as a fallback.
     */
    public Connection getConnection() throws SQLException {
        try {
            return ds.getConnection();
        } catch (SQLException firstEx) {
            LOGGER.log(Level.WARNING, "DataSource getConnection() failed: {0}", firstEx.getMessage());
            if (!jdbcUrl.isEmpty()) {
                try {
                    return DriverManager.getConnection(jdbcUrl, user, password);
                } catch (SQLException dmEx) {
                    firstEx.addSuppressed(dmEx);
                    LOGGER.log(Level.SEVERE, "DriverManager fallback failed: {0}", dmEx.getMessage());
                    throw dmEx;
                }
            }
            throw firstEx;
        }
    }

    /**
     * Test connection and throw SQLException on failure so caller can see cause.
     */
    public boolean testConnection() throws SQLException {
        try (Connection c = getConnection()) {
            return c != null && !c.isClosed();
        }
    }
}
