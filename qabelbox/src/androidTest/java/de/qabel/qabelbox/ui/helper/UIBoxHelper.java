package de.qabel.qabelbox.ui.helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.storage.StorageSearch;

/**
 * Created by danny on 18.01.16.
 */
public class UIBoxHelper {

    private final MainActivity mActivity;
    private final String TAG = this.getClass().getSimpleName();
    private LocalQabelService mService;
    private BoxProvider provider;
    public BoxVolume mBoxVolume;
    private boolean finished = false;

    public UIBoxHelper(MainActivity activity) {

        mActivity = activity;
    }

    public void bindService(final QabelBoxApplication app) {

        Intent serviceIntent = new Intent(app.getApplicationContext(), LocalQabelService.class);
        finished = false;
        app.bindService(serviceIntent, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                Log.d(TAG, "LocalQabelService connected");

                provider = app.getProvider();

                Log.i(TAG, "Provider: " + provider);
                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                mService = binder.getService();

                finished = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

                mService = null;
            }
        }, Context.BIND_AUTO_CREATE);
        while (!finished) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean deleteFile(Activity activity,Identity identity, String name, String targetFolder) {

        String keyIdentifier = identity.getEcPublicKey()
                .getReadableKeyIdentifier();
        Uri uploadUri = DocumentsContract.buildDocumentUri(
                BoxProvider.AUTHORITY, keyIdentifier + MainActivity.HARDCODED_ROOT + targetFolder + name);
        return DocumentsContract.deleteDocument(activity.getContentResolver(), uploadUri);
    }

    public boolean uploadFile(Identity identity, String name, byte[] data, String targetFolder) {

        Log.d(TAG, "upload demo file " + name);

        String keyIdentifier = identity.getEcPublicKey()
                .getReadableKeyIdentifier();
        Uri uploadUri = DocumentsContract.buildDocumentUri(
                BoxProvider.AUTHORITY, keyIdentifier + MainActivity.HARDCODED_ROOT + targetFolder + name);

        try {
            OutputStream outputStream = QabelBoxApplication.getInstance().getContentResolver().openOutputStream(uploadUri, "w");
            if (outputStream == null) {
                return false;
            }

            IOUtils.copy(new ByteArrayInputStream(data), outputStream);
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error opening output stream for upload", e);
        }

        return true;
    }

    public Identity addIdentity(final String identName) {

        URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
        DropServer dropServer = new DropServer(uri, "", true);
        DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator(2 * 8);
        DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
        Collection<DropURL> dropURLs = new ArrayList<>();
        dropURLs.add(dropURL);
        Identity identity = new Identity(identName,
                dropURLs, new QblECKeyPair());
        finished = false;

        Log.d(TAG, "identity added " + identity.getAlias() + " " + identity.getEcPublicKey().getReadableKeyIdentifier());
        mService.addIdentity(identity);
        mService.setActiveIdentity(identity);

        try {
            initBoxVolume(identity);
            mBoxVolume.navigate();
        } catch (QblStorageException e) {
            Log.e(TAG, "Cannot navigate to root", e);
            try {
                mBoxVolume.createIndex();
                mBoxVolume.navigate();
            } catch (QblStorageException e1) {
                Log.e(TAG, "Creating a volume failed", e1);
            }
        }

        return identity;
    }

    private void initBoxVolume(Identity activeIdentity) throws QblStorageException {

        mBoxVolume = provider.getVolumeForRoot(
                activeIdentity.getEcPublicKey().getReadableKeyIdentifier(),
                null, null);
        mBoxVolume.createIndex();
    }

    public void setActiveIdentity(Identity identity) {

        mService.setActiveIdentity(identity);
    }

    public void deleteIdentity(Identity identity) {

        mService.deleteIdentity(identity);
    }

    public Identity getCurrentIdentity() {

        return mService.getActiveIdentity();
    }

    /**
     * wait until the volume contain a defined count of files
     *
     * @param fileCount
     */
    public void waitUntilFileCount(int fileCount) {

        try {
            while (
                    new StorageSearch(mBoxVolume.navigate()).getResults().size() < fileCount) {
                Log.d(TAG, "wait until all files uploaded");
                Thread.sleep(500);
            }
        } catch (QblStorageException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
