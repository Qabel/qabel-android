package de.qabel.qabelbox.util;


import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.io.Serializable;
import java.util.List;

import de.qabel.core.config.Persistable;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.qabelbox.config.AndroidPersistence;
import de.qabel.qabelbox.config.QblSQLiteParams;

public class PersistenceTest extends AndroidTestCase {
    private final static char[] ENCRYPTION_PASSWORD = "password".toCharArray();
    private final static String DB_NAME = "qabel-android-test";
    private final static int DB_VERSION = 1;
    private AndroidPersistence persistence;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getContext().deleteDatabase(DB_NAME);
        QblSQLiteParams params = new QblSQLiteParams(getContext(), DB_NAME, null, DB_VERSION);
        persistence = new AndroidPersistence(params, ENCRYPTION_PASSWORD);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testOpenWrongPassword() {
        try {
            QblSQLiteParams params = new QblSQLiteParams(getContext(), DB_NAME, null, DB_VERSION);
            persistence = new AndroidPersistence(params, "wrongPassword".toCharArray());
        } catch (QblInvalidEncryptionKeyException e) {
            return;
        }
        Assert.fail();
    }

    public void testGetNotPersistedEntity() {
        Assert.assertNull(persistence.getEntity("1", PersistenceTestObject.class));
    }

    public void testGetEntities() {
        PersistenceTestObject pto = new PersistenceTestObject("pto");
        PersistenceTestObject pto2 = new PersistenceTestObject("pto2");

        Assert.assertTrue(persistence.persistEntity(pto));
        Assert.assertTrue(persistence.persistEntity(pto2));

        List<Persistable> objects = persistence.getEntities(PersistenceTestObject.class);
        Assert.assertEquals(2, objects.size());
        Assert.assertTrue(objects.contains(pto));
        Assert.assertTrue(objects.contains(pto2));
    }

    public void testGetEntitiesEmpty() {
        List<Persistable> objects = persistence.getEntities(PersistenceTestObject.class);
        Assert.assertEquals(0, objects.size());
    }

    public void testUpdateEntity() {
        PersistenceTestObject pto = new PersistenceTestObject("pto");
        pto.data = "changed";

        Assert.assertTrue(persistence.persistEntity(pto));
        Assert.assertTrue(persistence.updateEntity(pto));

        PersistenceTestObject receivedPto = (PersistenceTestObject) persistence.getEntity(pto.getPersistenceID(),
                PersistenceTestObject.class);
        Assert.assertEquals(pto, receivedPto);
    }

    public void testUpdateOrPersistEntity() {
        PersistenceTestObject pto = new PersistenceTestObject("pto");

        Assert.assertTrue(persistence.updateOrPersistEntity(pto));
        PersistenceTestObject receivedPto = (PersistenceTestObject) persistence.getEntity(pto.getPersistenceID(),
                PersistenceTestObject.class);
        Assert.assertEquals(pto, receivedPto);

        pto.data = "changed";
        Assert.assertTrue(persistence.updateOrPersistEntity(pto));
        receivedPto = (PersistenceTestObject) persistence.getEntity(pto.getPersistenceID(),
                PersistenceTestObject.class);
        Assert.assertEquals(pto, receivedPto);
    }

    public void testUpdateNotStoredEntity() {
        PersistenceTestObject pto = new PersistenceTestObject("pto");
        Assert.assertFalse(persistence.updateEntity(pto));
    }

    public void testPersistenceRoundTrip() {
        PersistenceTestObject pto = new PersistenceTestObject("pto");
        persistence.persistEntity(pto);

        // Assure that pto has been persisted
        PersistenceTestObject receivedPto = (PersistenceTestObject) persistence.getEntity(pto.getPersistenceID(),
                PersistenceTestObject.class);
        Assert.assertEquals(pto, receivedPto);

        Assert.assertTrue(persistence.removeEntity(pto.getPersistenceID(), PersistenceTestObject.class));

        PersistenceTestObject receivedPto2 = (PersistenceTestObject) persistence.getEntity(pto.getPersistenceID(),
                PersistenceTestObject.class);
        Assert.assertNull(receivedPto2);
    }

    public void testDropTable() {
        PersistenceTestObject pto = new PersistenceTestObject("pto");
        persistence.persistEntity(pto);

        Assert.assertTrue(persistence.dropTable(PersistenceTestObject.class));
        Assert.assertNull(persistence.getEntity(pto.getPersistenceID(), PersistenceTestObject.class));
    }

    public void testDropNotExistingTable() {
        Assert.assertFalse(persistence.dropTable(PersistenceTestObject.class));
    }

    public void testChangeMasterKey() {
        PersistenceTestObject pto = new PersistenceTestObject("pto");
        persistence.persistEntity(pto);

        Assert.assertTrue(persistence.changePassword(ENCRYPTION_PASSWORD, "qabel2".toCharArray()));

        PersistenceTestObject receivedPto = (PersistenceTestObject) persistence.getEntity(pto.getPersistenceID(),
                PersistenceTestObject.class);
        Assert.assertEquals(pto, receivedPto);
    }

    public void testChangeMasterKeyFail() {
        PersistenceTestObject pto = new PersistenceTestObject("pto");
        persistence.persistEntity(pto);

        Assert.assertFalse(persistence.changePassword("wrongPassword".toCharArray(), "qabel2".toCharArray()));

        PersistenceTestObject receivedPto = (PersistenceTestObject) persistence.getEntity(pto.getPersistenceID(),
                PersistenceTestObject.class);
        Assert.assertEquals(pto, receivedPto);
    }

    static public class PersistenceTestObject extends Persistable implements Serializable {
        private static final long serialVersionUID = -9721591389456L;
        public String data;

        public PersistenceTestObject(String data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
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
