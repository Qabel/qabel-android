package de.qabel.android.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.android.exceptions.QblStorageException;

class FileMetadata {
	private static final Logger logger = LoggerFactory.getLogger(FileMetadata.class.getName());
	private static final String JDBC_PREFIX = "jdbc:sqlite:";
	private static final String JDBC_CLASS = "org.sqldroid.SQLDroidDriver";
	private final Connection connection;

	private final File path;

	private final String[] initSql = {
			"CREATE TABLE spec_version (" +
					" version INTEGER PRIMARY KEY )",
			"CREATE TABLE file (" +
					" owner BLOB NOT NULL," +
					" prefix VARCHAR(255) NOT NULL," +
					" block VARCHAR(255) NOT NULL," +
					" name VARCHAR(255) NULL PRIMARY KEY," +
					" size LONG NOT NULL," +
					" mtime LONG NOT NULL," +
					" key BLOB NOT NULL )",
			"INSERT INTO spec_version (version) VALUES(0)"
	};

	public FileMetadata(QblECPublicKey owner, BoxFile boxFile, File tempDir) throws QblStorageException {
		try {
			path = File.createTempFile("dir", "db", tempDir);
		} catch (IOException e) {
			throw new QblStorageException(e);
		}
		try {
			Class.forName(JDBC_CLASS);
			connection = DriverManager.getConnection(JDBC_PREFIX + path.getAbsolutePath());
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("Cannot open database!", e);
		} catch (ClassNotFoundException e) {
			throw new QblStorageException(e);
		}
		try {
			initDatabase();
		} catch (SQLException e) {
			throw new RuntimeException("Cannot init the database", e);
		}
		insertFile(owner, boxFile);
	}

	public FileMetadata(File path) throws QblStorageException {
		this.path = path;
		try {
			Class.forName(JDBC_CLASS);
			connection = DriverManager.getConnection(JDBC_PREFIX + path.getAbsolutePath());
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("Cannot open database!", e);
		} catch (ClassNotFoundException e) {
			throw new QblStorageException(e);
		}
	}

	private void insertFile(QblECPublicKey owner, BoxFile boxFile) throws QblStorageException {
		try (PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO file (owner, prefix, block, name, size, mtime, key) VALUES(?, ?, ?, ?, ?, ?, ?)")) {
			statement.setBytes(1, owner.getKey());
			statement.setString(2, boxFile.prefix);
			statement.setString(3, boxFile.block);
			statement.setString(4, boxFile.name);
			statement.setLong(5, boxFile.size);
			statement.setLong(6, boxFile.mtime);
			statement.setBytes(7, boxFile.key);
			if (statement.executeUpdate() != 1) {
				throw new QblStorageException("Failed to insert file");
			}

		} catch (SQLException e) {
			logger.error("Could not insert file " + boxFile.name);
			throw new QblStorageException(e);
		}
	}

	public File getPath() {
		return path;
	}

	private void initDatabase() throws SQLException, QblStorageException {
		for (String q : initSql) {
			try (Statement statement = connection.createStatement()){
				statement.executeUpdate(q);
			}
		}
	}

	Integer getSpecVersion() throws QblStorageException {
		try (Statement statement = connection.createStatement()) {
			ResultSet rs = statement.executeQuery(
					"SELECT version FROM spec_version");
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new QblStorageException("No version found!");
			}
		} catch (SQLException e) {
			throw new QblStorageException(e);
		}
	}

	BoxExternalFile getFile() throws QblStorageException {
		try (Statement statement = connection.createStatement()) {
			ResultSet rs = statement.executeQuery("SELECT owner, prefix, block, name, size, mtime, key FROM file LIMIT 1");
			if (rs.next()) {
				return new BoxExternalFile(new QblECPublicKey(rs.getBytes(1)), rs.getString(2), rs.getString(3),
						rs.getString(4), rs.getLong(5), rs.getLong(6), rs.getBytes(7));
			}
			return null;
		} catch (SQLException e) {
			throw new QblStorageException(e);
		}
	}
}

