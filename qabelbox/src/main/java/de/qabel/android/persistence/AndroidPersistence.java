package de.qabel.android.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.qabel.android.exceptions.QblPersistenceException;
import de.qabel.core.config.Persistable;
import de.qabel.core.config.Persistence;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;

public class AndroidPersistence extends Persistence<QblSQLiteParams> {

	private DatabaseWrapper databaseWrapper;

	private static final Logger LOGGER = LoggerFactory.getLogger(AndroidPersistence.class.getName());

	public AndroidPersistence(QblSQLiteParams params) throws QblInvalidEncryptionKeyException {
		this.databaseWrapper = createDatabaseWrapper(params);
		connect(params);
	}

	protected DatabaseWrapper createDatabaseWrapper(QblSQLiteParams params) {
		return new AndroidDatabaseWrapper(params);
	}

	@Override
	protected boolean connect(QblSQLiteParams qblSQLiteParams) {
		return databaseWrapper.connect();
	}

	@Override
	public boolean persistEntity(Persistable object) throws QblPersistenceException {

		String sql = "CREATE TABLE IF NOT EXISTS " +
				getTableNameForClass(object.getClass()) +
				"(ID TEXT PRIMARY KEY NOT NULL," +
				"BLOB BLOB NOT NULL)";

		try {
			databaseWrapper.execSQL(sql);
		} catch (QblPersistenceException e) {
			e.printStackTrace();
			LOGGER.error("Cannot create table!", e.getException());
		}

		databaseWrapper.insert(object);
		return true;
	}

	@Override
	public boolean updateEntity(Persistable object) throws QblPersistenceException {
		if (object == null) {
			throw new IllegalArgumentException("Arguments cannot be null!");
		}

		if (getEntity(object.getPersistenceID(), object.getClass()) == null) {
			LOGGER.info("Entity not stored!");
			throw new QblPersistenceException();
		}

		databaseWrapper.update(object);
		return true;
	}

	@Override
	public boolean updateOrPersistEntity(Persistable object) throws QblPersistenceException {
		if (getEntity(object.getPersistenceID(), object.getClass()) == null) {
			return persistEntity(object);
		} else {
			return updateEntity(object);
		}
	}

	@Override
	public boolean removeEntity(String id, Class cls) throws QblPersistenceException {
		databaseWrapper.delete(id, cls);
		return true;
	}

	@Override
	public <U extends Persistable> U getEntity(String id, Class<? extends U> cls) throws QblPersistenceException {
		try {
			return databaseWrapper.getEntity(id, cls);
		} catch (QblPersistenceException e) {
			LOGGER.debug("Couldn't get entity! " + e.getException().getLocalizedMessage());
			throw e;
		}
	}

	@Override
	public <U extends Persistable> List<U> getEntities(Class<? extends U> cls) throws QblPersistenceException {
		try {
			return databaseWrapper.getEntities(cls);
		} catch (QblPersistenceException e) {
			LOGGER.info("Table does not exist!");
			throw e;
		}
	}

	@Override
	public boolean dropTable(Class cls) throws QblPersistenceException {
		try {
			databaseWrapper.execSQL("DROP TABLE " + getTableNameForClass(cls));
		} catch (QblPersistenceException e) {
			LOGGER.info("Table does not exist!");
			throw e;
		}
		return true;
	}

	private static String getTableNameForClass(Class cls) {
		return '\'' + cls.getCanonicalName() + '\'';
	}
}
