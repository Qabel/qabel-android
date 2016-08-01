package de.qabel.core.repositories;

import org.sqldroid.SQLDroidConnection;
import org.sqldroid.SQLDroidPreparedStatement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GeneratedKeysPreparedStatement extends SQLDroidPreparedStatement implements PreparedStatement {
    public GeneratedKeysPreparedStatement(String sql, SQLDroidConnection sqldroid) {
        super(sql, sqldroid);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        PreparedStatement statement = sqldroidConnection.prepareStatement(
                "SELECT last_insert_rowid()");
		return statement.executeQuery();
    }
}
