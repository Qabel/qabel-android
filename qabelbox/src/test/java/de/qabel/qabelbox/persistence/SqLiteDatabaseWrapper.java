package de.qabel.android.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.qabel.core.config.Persistable;
import de.qabel.android.exceptions.QblPersistenceException;

public class SqLiteDatabaseWrapper extends DatabaseWrapperImpl<String> {

	private static final String INSERT_STMT = "INSERT INTO %s (" + STR_ID + ", " + STR_BLOB + ") VALUES (:id, :value)";
	private static final String UPDATE_STMT = "UPDATE %s SET " + STR_BLOB + " = :value WHERE " + STR_ID + " = :id";
	private static final String DELETE_STMT = "DELETE FROM %s WHERE " + STR_ID + " = :id";

	private Connection connection;

	@Override
	public boolean connect() {
		try {
			//
			Class.forName("org.sqlite.JDBC");
			//Use temporary memory database
			connection = DriverManager.getConnection("jdbc:sqlite::memory:");
			return true;
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			return false;
		}
	}

	public void disconnect() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void execSQL(String sql) throws QblPersistenceException {
		Statement statement;
		try {
			statement = connection.createStatement();
			statement.execute(sql);
			statement.close();
		} catch (SQLException e) {
			throw new QblPersistenceException(e);
		}
	}

	@Override
	public boolean insert(Persistable entity) throws QblPersistenceException {
		PreparedStatement statement = null;
		try {
			String sql = String.format(INSERT_STMT, PersistenceUtil.getTableNameForClass(entity.getClass()));
			statement = connection.prepareStatement(sql);
			statement.setString(1, entity.getPersistenceID());
			statement.setBytes(2, PersistenceUtil.serialize(entity.getPersistenceID(), entity));
			return statement.executeUpdate() == 1;
		} catch (SQLException e) {
			throw new QblPersistenceException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	@Override
	public boolean update(Persistable entity) throws QblPersistenceException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(String.format(UPDATE_STMT, PersistenceUtil.getTableNameForClass(entity.getClass())));
			statement.setBytes(1, PersistenceUtil.serialize(entity.getPersistenceID(), entity));
			statement.setString(2, entity.getPersistenceID());
			return statement.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new QblPersistenceException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	@Override
	public boolean delete(String id, Class cls) throws QblPersistenceException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(String.format(DELETE_STMT, PersistenceUtil.getTableNameForClass(cls)));
			statement.setString(1, id);
			return statement.executeUpdate() == 1;
		} catch (SQLException e) {
			throw new QblPersistenceException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	@Override
	public <U extends Persistable> U getEntity(String id, Class<? extends U> cls) throws QblPersistenceException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement("SELECT " + STR_BLOB + " FROM " + PersistenceUtil.getTableNameForClass(cls) + " WHERE " + STR_ID_QUERY);
			statement.setString(1, id);
			ResultSet result = statement.executeQuery();
			if (result.next()) {
				return (U) PersistenceUtil.deserialize(id, result.getBytes(1));
			}
		} catch (SQLException e) {
			throw new QblPersistenceException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
		return null;
	}

	@Override
	public <U extends Persistable> List<U> getEntities(Class<? extends U> cls) throws QblPersistenceException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement("SELECT " + STR_ID + ", " + STR_BLOB + " FROM " + PersistenceUtil.getTableNameForClass(cls));
			ResultSet result = statement.executeQuery();
			List<U> entityList = new ArrayList<>();
			while (result.next()) {
				entityList.add((U) PersistenceUtil.deserialize(result.getString(1), result.getBytes(2)));
			}
			return entityList;
		} catch (SQLException e) {
			throw new QblPersistenceException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}
}
