package de.qabel.qabelbox.ui.files;

import android.content.Intent;
import android.support.test.espresso.Espresso;

import java.util.List;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.ui.AbstractUITest;
import de.qabel.qabelbox.ui.idling.InjectedIdlingResource;

public abstract class FilesFragmentUITestBase extends AbstractUITest {

    protected boolean autoLaunch = true;
    private InjectedIdlingResource idlingResource;

    protected class ExampleFile {

        private String name;
        private byte[] data;

        public ExampleFile(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public byte[] getData() {
            return data;
        }

    }

    protected InjectedIdlingResource getIdlingResource() {
        return idlingResource;
    }

    @Override
    public void setUp() throws Throwable {
        super.setUp();
        setupData();
        if(autoLaunch) {
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.putExtra(MainActivity.ACTIVE_IDENTITY, mBoxHelper.
                    getCurrentIdentity().getKeyIdentifier());
            launchActivity(intent);
        }
        idlingResource = new InjectedIdlingResource();
        Espresso.registerIdlingResources(idlingResource);
    }

    @Override
    public void cleanUp() {
        if(idlingResource != null){
            Espresso.unregisterIdlingResources(idlingResource);
        }
        super.cleanUp();
    }

    protected void addExampleFiles(Identity identity, List<ExampleFile> files) throws Exception {
        for (ExampleFile exampleFile : files) {
            mBoxHelper.uploadFile(identity, exampleFile.getName(), exampleFile.getData(), "");
        }
        mBoxHelper.waitUntilFileCount(identity, files.size());
    }

    protected abstract void setupData() throws Exception;

}
