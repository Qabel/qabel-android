package de.qabel.qabelbox.providers;

import android.content.Context;

import de.qabel.qabelbox.services.LocalQabelService;

public class MockedInternalContentProvider extends InternalContentProvider {
    public MockedInternalContentProvider(Context context) {
        super(context);
    }

    public void injectService(LocalQabelService service) {
        mService = service;
        initDatabases();
    }


}
