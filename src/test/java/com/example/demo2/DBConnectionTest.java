package com.example.demo2;

import com.example.demo2.dal.DBManager;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DBConnectionTest {

    @Test
    public void testDatabaseConnection() throws Exception {
        Connection conn = null;
        try {
            conn = DBManager.getConnection();
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Test
    public void testTablesExist() throws Exception {
        Connection conn = null;
        try {
            conn = DBManager.getConnection();
            String[] tables = {"songs", "playlists", "playlist_songs"};
            
            for (String table : tables) {
                try (Statement st = conn.createStatement()) {
                    ResultSet rs = st.executeQuery(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + table + "'"
                    );
                    assertTrue(rs.next(), "Table '" + table + "' should exist");
                }
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}
