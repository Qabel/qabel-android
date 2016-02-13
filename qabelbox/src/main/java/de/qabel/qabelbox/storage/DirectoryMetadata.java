package de.qabel.qabelbox.storage;

import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNameConflict;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class DirectoryMetadata {
	private static final Logger logger = LoggerFactory.getLogger(DirectoryMetadata.class.getName());
	private static final String JDBC_PREFIX = "jdbc:sqlite:";
	private static final String JDBC_CLASS = "org.sqldroid.SQLDroidDriver";
	public static final int TYPE_NONE = -1;
	private final Connection connection;

	private final String fileName;
	byte[] deviceId;
	String root;
	File path;

	private static final int TYPE_FILE = 0;
	private static final int TYPE_FOLDER = 1;
	private static final int TYPE_EXTERNAL = 2;

	private final String[] initSql = {
			"CREATE TABLE meta (" +
					" name VARCHAR(24) PRIMARY KEY," +
					" value TEXT )",
			"CREATE TABLE spec_version (" +
					" version INTEGER PRIMARY KEY )",
			"CREATE TABLE version (" +
					" id INTEGER PRIMARY KEY," +
					" version BLOB NOT NULL," +
					" time LONG NOT NULL )",
			"CREATE TABLE shares (" +
					" id INTEGER PRIMARY KEY," +
					" ref VARCHAR(255) NOT NULL," +
					" recipient BLOB NOT NULL," +
					" type INTEGER NOT NULL )",
			"CREATE TABLE files (" +
					" block VARCHAR(255) NOT NULL," +
					" name VARCHAR(255) NULL PRIMARY KEY," +
					" size LONG NOT NULL," +
					" mtime LONG NOT NULL," +
					" key BLOB NOT NULL, " +
					" meta VARCHAR(255), " +
					" metakey BLOB )",
			"CREATE TABLE folders (" +
					" ref VARCHAR(255)NOT NULL," +
					" name VARCHAR(255)NOT NULL PRIMARY KEY," +
					" key BLOB NOT NULL )",
			"CREATE TABLE externals (" +
					" is_folder BOOLEAN NOT NULL," +
					" owner BLOB NOT NULL," +
					" name VARCHAR(255)NOT NULL PRIMARY KEY," +
					" key BLOB NOT NULL," +
					" url TEXT NOT NULL )",
			"INSERT INTO spec_version (version) VALUES(0)"
	};
	private final File tempDir;

	public DirectoryMetadata(Connection connection, String root, byte[] deviceId,
	                         File path, String fileName, File tempDir) {
		this.connection = connection;
		this.root = root;
		this.deviceId = deviceId;
		this.path = path;
		this.fileName = fileName;
		this.tempDir = tempDir;
	}

	public DirectoryMetadata(Connection connection, byte[] deviceId, File path, String fileName,
	                         File tempDir) {
		this.connection = connection;
		this.deviceId = deviceId;
		this.path = path;
		this.fileName = fileName;
		this.tempDir = tempDir;
	}

	static DirectoryMetadata newDatabase(String root, byte[] deviceId, File tempDir) throws QblStorageException {
		File path;
		try {
			path = File.createTempFile("dir", "db", tempDir);
		} catch (IOException e) {
			throw new QblStorageException(e);
		}
		Connection connection;
		try {
			Class.forName(JDBC_CLASS);
			connection = DriverManager.getConnection(JDBC_PREFIX + path.getAbsolutePath());
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("Cannot open database!", e);
		} catch (ClassNotFoundException e) {
			throw new QblStorageException(e);
		}
		DirectoryMetadata dm = new DirectoryMetadata(connection, root, deviceId, path,
				UUID.randomUUID().toString(), tempDir);
		try {
			dm.initDatabase();
		} catch (SQLException e) {
			throw new RuntimeException("Cannot init the database", e);
		}
		return dm;
	}

	static DirectoryMetadata openDatabase(File path, byte[] deviceId, String fileName, File tempDir) throws QblStorageException {
		Connection connection;
		try {
			Class.forName(JDBC_CLASS);
			connection = DriverManager.getConnection(JDBC_PREFIX + path.getAbsolutePath());
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("Cannot open database!", e);
		} catch (ClassNotFoundException e) {
			throw new QblStorageException(e);
		}
		return new DirectoryMetadata(connection, deviceId, path, fileName, tempDir);
	}

	public File getPath() {
		return path;
	}

	public String getFileName() {
		return fileName;
	}

	private void initDatabase() throws SQLException, QblStorageException {
		for (String q : initSql) {
			Statement statement = null;
			try {
				statement = connection.createStatement();
				statement.executeUpdate(q);
			} finally {
				if (statement != null) {
					statement.close();
				}
			}
		}
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(
					"INSERT INTO version (version, time) VALUES (?, ?)");
			statement.setBytes(1, initVersion());
			statement.setLong(2, System.currentTimeMillis());
			statement.executeUpdate();
		} finally {
			statement.close();
		}
		setLastChangedBy();
		// only set root if this actually has a root attribute
		// (only index metadata files have it)
		if (root != null) {
			setRoot(root);
		}
	}

	private void setRoot(String root) throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(
					"INSERT OR REPLACE INTO meta (name, value) VALUES ('root', ?)");
			statement.setString(1, root);
			statement.executeUpdate();
		} finally {
			if (statement != null) {
				statement.close();
			}
		}
	}

	String getRoot() throws QblStorageException {
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(
					"SELECT value FROM meta WHERE name='root'");
			if (rs.next()) {
				return rs.getString(1);
			} else {
				throw new QblStorageException("No root found!");
			}
		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	Integer getSpecVersion() throws QblStorageException {
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(
					"SELECT version FROM spec_version");
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new QblStorageException("No version found!");
			}
		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
				}
			}
		}
	}

	void setLastChangedBy() throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(
					"INSERT OR REPLACE INTO meta (name, value) VALUES ('last_change_by', ?)");
			String x = new String(Hex.encode(deviceId));
			statement.setString(1, x);
			statement.executeUpdate();
		} finally {
			if (statement != null) {
				statement.close();
			}
		}

	}

	byte[] getLastChangedBy() throws QblStorageException {
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(
					"SELECT value FROM meta WHERE name='last_change_by'");
			if (rs.next()) {
				String lastChanged = rs.getString(1);
				return Hex.decode(lastChanged);
			} else {
				throw new QblStorageException("No version found!");
			}
		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	private byte[] initVersion() throws QblStorageException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new QblStorageException(e);
		}
		md.update(new byte[]{0, 0});
		md.update(deviceId);
		return md.digest();
	}

	byte[] getVersion() throws QblStorageException {
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(
					"SELECT version FROM version ORDER BY id DESC LIMIT 1");
			if (rs.next()) {
				return rs.getBytes(1);
			} else {
				throw new QblStorageException("No version found!");
			}
		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	void commit() throws QblStorageException {
		byte[] oldVersion = getVersion();
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new QblStorageException(e);
		}
		md.update(new byte[]{0, 1});
		md.update(oldVersion);
		md.update(deviceId);
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(
					"INSERT INTO version (version, time) VALUES (?, ?)");
			statement.setBytes(1, md.digest());
			statement.setLong(2, System.currentTimeMillis());
			if (statement.executeUpdate() != 1) {
				throw new QblStorageException("Could not update version!");
			}
			setLastChangedBy();
		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
				}
			}
		}
	}


	List<BoxFile> listFiles() throws QblStorageException {
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(
					"SELECT block, name, size, mtime, key, meta, metakey FROM files");
			List<BoxFile> files = new ArrayList<>();
			while (rs.next()) {
				files.add(new BoxFile(rs.getString(1),
						rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getBytes(5), rs.getString(6), rs.getBytes(7)));
			}
			return files;
		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}


	void insertFile(BoxFile file) throws QblStorageException {
		int type = isA(file.name);
		if ((type != TYPE_NONE) && (type != TYPE_FILE)) {
			throw new QblStorageNameConflict(file.name);
		}
		;
		PreparedStatement st = null;
		try {
			st = connection.prepareStatement(
					"INSERT INTO files (block, name, size, mtime, key, meta, metakey) VALUES(?, ?, ?, ?, ?, ?, ?)");
			st.setString(1, file.block);
			st.setString(2, file.name);
			st.setLong(3, file.size);
			st.setLong(4, file.mtime);
			st.setBytes(5, file.key);
			st.setString(6, file.meta);
			st.setBytes(7, file.metakey);
			if (st.executeUpdate() != 1) {
				throw new QblStorageException("Failed to insert file");
			}

		} catch (SQLException e) {
			logger.error("Could not insert file " + file.name);
			throw new QblStorageException(e);
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	void deleteFile(BoxFile file) throws QblStorageException {
		try {
			PreparedStatement st = connection.prepareStatement(
					"DELETE FROM files WHERE name=?");
			st.setString(1, file.name);
			if (st.executeUpdate() != 1) {
				throw new QblStorageException("Failed to delete file: Not found");
			}

		} catch (SQLException e) {
			throw new QblStorageException(e);
		}
	}

	List<BoxExternalReference> listExternalReferences() throws QblStorageException {
		try (Statement statement = connection.createStatement()){
			ResultSet rs = statement.executeQuery(
					"SELECT is_folder, url, name, owner, key FROM externals");
			List<BoxExternalReference> files = new ArrayList<>();
			while (rs.next()) {
				files.add(
						new BoxExternalReference(rs.getBoolean(1), rs.getString(2), rs.getString(3),
								new QblECPublicKey(rs.getBytes(4)), rs.getBytes(5)));
			}
			return files;
		} catch (SQLException e) {
			throw new QblStorageException(e);
		}
	}

	void insertExternalReference(BoxExternalReference file) throws QblStorageException {
		int type = isA(file.name);
		if ((type != TYPE_NONE) && (type != TYPE_FILE)) {
			throw new QblStorageNameConflict(file.name);
		}
		try (PreparedStatement st = connection.prepareStatement(
			 "INSERT INTO externals (is_folder, url, name, owner, key) VALUES(?, ?, ?, ?, ?)")){
			st.setBoolean(1, file.isFolder);
			st.setString(2, file.url);
			st.setString(3, file.name);
			st.setBytes(4, file.owner.getKey());
			st.setBytes(5, file.key);
			if (st.executeUpdate() != 1) {
				throw new QblStorageException("Failed to insert file");
			}

		} catch (SQLException e) {
			logger.error("Could not insert file " + file.name);
			throw new QblStorageException(e);
		}
	}

	void deleteExternalReference(String name) throws QblStorageException {
		try (PreparedStatement st = connection.prepareStatement(
				"DELETE FROM externals WHERE name=?")){
			st.setString(1, name);
			if (st.executeUpdate() != 1) {
				throw new QblStorageException("Failed to delete file: Not found");
			}

		} catch (SQLException e) {
			throw new QblStorageException(e);
		}
	}

	void insertFolder(BoxFolder folder) throws QblStorageException {
		int type = isA(folder.name);
		if ((type != TYPE_NONE) && (type != TYPE_FOLDER)) {
			throw new QblStorageNameConflict(folder.name);
		}
		try {
			PreparedStatement st = connection.prepareStatement(
					"INSERT INTO folders (ref, name, key) VALUES(?, ?, ?)");
			st.setString(1, folder.ref);
			st.setString(2, folder.name);
			st.setBytes(3, folder.key);
			if (st.executeUpdate() != 1) {
				throw new QblStorageException("Failed to insert folder");
			}

		} catch (SQLException e) {
			throw new QblStorageException(e);
		}
	}

	void deleteFolder(BoxFolder folder) throws QblStorageException {
		PreparedStatement st = null;
		try {
			st = connection.prepareStatement(
					"DELETE FROM folders WHERE name=?");
			st.setString(1, folder.name);
			if (st.executeUpdate() != 1) {
				throw new QblStorageException("Failed to insert folder");
			}

		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	List<BoxFolder> listFolders() throws QblStorageException {
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(
					"SELECT ref, name, key FROM folders");
			List<BoxFolder> folders = new ArrayList<>();
			while (rs.next()) {
				folders.add(new BoxFolder(rs.getString(1), rs.getString(2), rs.getBytes(3)));
			}
			return folders;
		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	BoxFile getFile(String name) throws QblStorageException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(
					"SELECT block, name, size, mtime, key, meta, metakey FROM files WHERE name=?");
			statement.setString(1, name);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				return new BoxFile(rs.getString(1),
						rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getBytes(5), rs.getString(6), rs.getBytes(7));
			}
			return null;
		} catch (SQLException e) {
			throw new QblStorageException(e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	int isA(String name) throws QblStorageException {
		String[] types = {"files", "folders", "externals"};
		for (int type = 0; type < 3; type++) {
			PreparedStatement statement = null;
			try {
				statement = connection.prepareStatement(
						"SELECT name FROM " + types[type] + " WHERE name=?");
				statement.setString(1, name);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					return type;
				}
			} catch (SQLException e) {
				throw new QblStorageException(e);
			} finally {
				try {
					if (statement != null) {
						statement.close();
					}
				} catch (SQLException e) {
				}
			}
		}
		return TYPE_NONE;
	}

	public File getTempDir() {
		return tempDir;


	}
}

