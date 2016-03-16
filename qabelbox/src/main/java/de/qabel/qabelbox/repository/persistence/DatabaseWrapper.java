package de.qabel.qabelbox.repository.persistence;

import java.util.List;

import de.qabel.core.config.Persistable;
import de.qabel.qabelbox.exceptions.QblPersistenceException;

public interface DatabaseWrapper {

	boolean connect();

	void execSQL(String sql) throws QblPersistenceException;

	boolean insert(Persistable entity) throws QblPersistenceException;

	boolean update(Persistable entity) throws QblPersistenceException;

	boolean delete(String id, Class cls) throws QblPersistenceException;

	<U extends Persistable> U  getEntity(String id, Class<? extends U> cls) throws QblPersistenceException;

	<U extends Persistable> List<U> getEntities(Class<? extends U> cls) throws QblPersistenceException;

}
