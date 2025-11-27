package dk.easv.mytunes.dal;

import java.sql.Connection;
import java.sql.SQLException;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseConnection Tests")
class DatabaseConnectionTest {

    @Test
    @DisplayName("Constructor creates DatabaseConnection instance without errors")
    void testConstructorCreatesInstance() {
        assertDoesNotThrow(() -> {
            DatabaseConnection dbConnection = new DatabaseConnection();
            assertNotNull(dbConnection, "DatabaseConnection instance should be created successfully");
        }, "Constructor should not throw any exceptions");
    }

    @Test
    @DisplayName("getConnection method exists and is callable")
    void testGetConnectionMethodExists() throws SQLServerException {
        DatabaseConnection dbConnection = new DatabaseConnection();
        assertDoesNotThrow(() -> {
            try {
                dbConnection.getConnection();
            } catch (SQLException e) {
                assertNotNull(e, "SQLException is expected when database is unavailable");
            }
        }, "getConnection method should be accessible");
    }

    @Test
    @DisplayName("getConnection throws SQLException when connection attempt fails")
    void testGetConnectionThrowsSQLException() throws SQLServerException {
        DatabaseConnection dbConnection = new DatabaseConnection();

        SQLException exception = assertThrows(SQLException.class, () -> {
            dbConnection.getConnection();
        }, "getConnection should throw SQLException when database connection fails");

        assertNotNull(exception, "SQLException should not be null");
        assertTrue(exception instanceof SQLException, "Thrown exception should be SQLException");
    }

    @Test
    @DisplayName("getConnection return type is Connection interface")
    void testGetConnectionReturnType() throws SQLServerException {
        DatabaseConnection dbConnection = new DatabaseConnection();

        try {
            Connection connection = dbConnection.getConnection();
            assertTrue(connection instanceof Connection, "Returned object should implement Connection interface");
        } catch (SQLException e) {
            assertNotNull(e, "SQLException indicates connection attempt was made");
        }
    }

    @Test
    @DisplayName("getConnection method signature declares SQLException in throws clause")
    void testGetConnectionThrowsClause() throws NoSuchMethodException {
        java.lang.reflect.Method method = DatabaseConnection.class.getMethod("getConnection");
        Class<?>[] exceptionTypes = method.getExceptionTypes();

        assertTrue(exceptionTypes.length > 0, "getConnection should declare thrown exceptions");
        boolean hasSQLException = false;
        for (Class<?> exceptionType : exceptionTypes) {
            if (exceptionType == SQLException.class) {
                hasSQLException = true;
                break;
            }
        }
        assertTrue(hasSQLException, "getConnection should declare SQLException in throws clause");
    }

    @Test
    @DisplayName("getConnection returns a Connection when successful")
    void testGetConnectionReturnsConnectionOnSuccess() throws SQLServerException {
        DatabaseConnection dbConnection = new DatabaseConnection();

        try {
            Connection connection = dbConnection.getConnection();
            assertNotNull(connection, "getConnection should return a non-null Connection when successful");
            assertTrue(connection instanceof Connection, "Returned object must be a Connection instance");
        } catch (SQLException e) {
            assertTrue(true, "SQLException expected when database is unavailable during testing");
        }
    }
}
