package de.qabel.qabelbox.services;

import android.content.Intent;
import android.test.ServiceTestCase;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;

public class LocalQabelServiceTest extends ServiceTestCase<LocalQabelService> {

	private LocalQabelService mService;
	private Identity identity;

	public LocalQabelServiceTest() {
		super(LocalQabelService.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		getContext().deleteDatabase(LocalQabelService.DB_NAME);
		Intent intent = new Intent(getContext(), LocalQabelService.class);
		startService(intent);
		this.mService = getService();
		identity = new Identity("foo", null, new QblECKeyPair());
		mService.addIdentity(identity);
		mService.setActiveIdentity(identity);
	}

	public void testRetrieveIdentity() {
		Identities identities = mService.getIdentities();
		assertTrue(identities.getIdentities().contains(identity));
	}

	public void testGetActiveIdentity() {
		assertEquals(identity, mService.getActiveIdentity());
	}

	public void testDeleteIdentity() {
		mService.deleteIdentity(identity);
		Identities identities = mService.getIdentities();
		assertFalse(identities.getIdentities().contains(identity));
		assertNull(mService.getActiveIdentity());
	}

	public void testModifyIdentity() {
	    identity.setAlias("bar");
		mService.modifyIdentity(identity);
		assertEquals(identity.getAlias(), mService.getActiveIdentity().getAlias());
	}

}
