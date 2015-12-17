package de.qabel.qabelbox.services;

import android.content.Intent;
import android.test.ServiceTestCase;

public class LocalQabelServiceTest extends ServiceTestCase<LocalQabelService> {

	private LocalQabelService mService;

	public LocalQabelServiceTest() {
		super(LocalQabelService.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Intent intent = new Intent(getContext(), LocalQabelService.class);
		startService(intent);
		this.mService = getService();
	}

	public void testOnCreateCalled() {
		assertNotNull(mService.getResourceActor());
	}

}
