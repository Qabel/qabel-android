package de.qabel.android.persistence;

import de.qabel.core.config.Persistable;
import de.qabel.android.exceptions.QblPersistenceException;

import java.util.List;

public interface DatabaseWrapper {

	boolean connect();

	void execSQL(String sql) throws QblPersistenceException;

	void insert(Persistable entity) throws QblPersistenceException;

	void update(Persistable entity) throws QblPersistenceException;

	void delete(String id, Class cls) throws QblPersistenceException;

	<U extends Persistable> U  getEntity(String id, Class<? extends U> cls) throws QblPersistenceException;

	<U extends Persistable> List<U> getEntities(Class<? extends U> cls) throws QblPersistenceException;

}
