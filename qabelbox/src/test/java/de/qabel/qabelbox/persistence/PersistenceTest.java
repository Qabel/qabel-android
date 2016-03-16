package de.qabel.qabelbox.repository.persistence;

import de.qabel.core.config.Persistable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;

import static org.junit.Assert.*;

public class PersistenceTest {
	private final static String DB_NAME = "qabel-android-test";
	private final static int DB_VERSION = 1;

	private AndroidPersistence persistence;
	private SqLiteDatabaseWrapper persistenceAdapter;

	@Before
	public void setUp() throws Exception {
		persistenceAdapter = new SqLiteDatabaseWrapper();
		persistence = new AndroidPersistence(null) {
			@Override
			protected DatabaseWrapper createDatabaseWrapper(QblSQLiteParams params) {
				return persistenceAdapter;
			}
		};
	}

	@After
	public void tearDown() throws Exception {
		persistenceAdapter.disconnect();
	}

	@Test
	public void testGetNotPersistedEntity() {
		assertNull(persistence.getEntity("1", PersistenceTestObject.class));
	}

	@Test
	public void testGetEntities() {
		PersistenceTestObject pto = new PersistenceTestObject("pto");
		PersistenceTestObject pto2 = new PersistenceTestObject("pto2");

		assertTrue(persistence.persistEntity(pto));
		assertTrue(persistence.persistEntity(pto2));

		List<PersistenceTestObject> objects = persistence.getEntities(PersistenceTestObject.class);
		assertEquals(2, objects.size());
		assertTrue(objects.contains(pto));
		assertTrue(objects.contains(pto2));
	}

	@Test
	public void testGetEntitiesEmpty() {
		List<PersistenceTestObject> objects = persistence.getEntities(PersistenceTestObject.class);
		assertEquals(0, objects.size());
	}

	@Test
	public void testUpdateEntity() {
		PersistenceTestObject pto = new PersistenceTestObject("pto");
		pto.data = "changed";

		assertTrue(persistence.persistEntity(pto));
		assertTrue(persistence.updateEntity(pto));

		PersistenceTestObject receivedPto = persistence.getEntity(pto.getPersistenceID(),
				PersistenceTestObject.class);
		assertEquals(pto, receivedPto);
	}

	@Test
	public void testUpdateOrPersistEntity() {
		PersistenceTestObject pto = new PersistenceTestObject("pto");

		assertTrue(persistence.updateOrPersistEntity(pto));
		PersistenceTestObject receivedPto = persistence.getEntity(pto.getPersistenceID(),
				PersistenceTestObject.class);
		assertEquals(pto, receivedPto);

		pto.data = "changed";
		assertTrue(persistence.updateOrPersistEntity(pto));
		receivedPto = persistence.getEntity(pto.getPersistenceID(),
				PersistenceTestObject.class);
		assertEquals(pto, receivedPto);
	}

	@Test
	public void testUpdateNotStoredEntity() {
		PersistenceTestObject pto = new PersistenceTestObject("pto");
		assertFalse(persistence.updateEntity(pto));
	}

	@Test
	public void testPersistenceRoundTrip() {
		PersistenceTestObject pto = new PersistenceTestObject("pto");
		persistence.persistEntity(pto);

		// Assure that pto has been persisted
		PersistenceTestObject receivedPto = persistence.getEntity(pto.getPersistenceID(),
				PersistenceTestObject.class);
		assertEquals(pto, receivedPto);

		assertTrue(persistence.removeEntity(pto.getPersistenceID(), PersistenceTestObject.class));

		PersistenceTestObject receivedPto2 = persistence.getEntity(pto.getPersistenceID(),
				PersistenceTestObject.class);
		assertNull(receivedPto2);
	}

	@Test
	public void testDropTable() {
		PersistenceTestObject pto = new PersistenceTestObject("pto");
		persistence.persistEntity(pto);

		assertTrue(persistence.dropTable(PersistenceTestObject.class));
		assertNull(persistence.getEntity(pto.getPersistenceID(), PersistenceTestObject.class));
	}

	@Test
	public void testDropNotExistingTable() {
		assertFalse(persistence.dropTable(PersistenceTestObject.class));
	}

	static public class PersistenceTestObject extends Persistable implements Serializable {
		private static final long serialVersionUID = -9721591389456L;
		public String data;

		public PersistenceTestObject(String data) {
			this.data = data;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			PersistenceTestObject that = (PersistenceTestObject) o;

			return !(data != null ? !data.equals(that.data) : that.data != null);
		}

		@Override
		public int hashCode() {
			return data != null ? data.hashCode() : 0;
		}
	}
}
