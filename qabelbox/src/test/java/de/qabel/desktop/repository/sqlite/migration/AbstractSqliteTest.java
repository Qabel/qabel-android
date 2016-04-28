package de.qabel.desktop.repository.sqlite.migration;

import de.qabel.desktop.repository.sqlite.DesktopClientDatabase;
import org.junit.After;
import org.junit.Before;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class AbstractSqliteTest {
    protected Connection connection;

    @Before
    public void setUp() throws Exception {
        connect();
    }

    public void connect() throws SQLException {
        // I don't have any idea why that is necessary
        int tries = 10;
        while (connection == null) {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            } catch (SQLException e) {
                if (tries-- <= 0) {
                    throw e;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA FOREIGN_KEYS = ON");
        }
    }

    public void reconnect() throws SQLException {
        connection.close();
        connect();
    }

    @After
    public void tearDown() throws Exception {
        connection.close();
    }

    protected boolean tableExists(String tableName) throws SQLException {
        return new DesktopClientDatabase(connection).tableExists(tableName);
    }
}
