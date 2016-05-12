package de.qabel.qabelbox.ui.helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import de.qabel.core.config.Contact;
import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.VolumeFileTransferHelper;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.RealTokerGetter;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.storage.StorageSearch;

/**
 * Created by danny on 18.01.16.
 */
public class UIBoxHelper {


    private final String TAG = this.getClass().getSimpleName();
    private LocalQabelService mService;
    private BoxProvider provider;
    public BoxVolume mBoxVolume;
    private boolean finished = false;

    public UIBoxHelper(Context activity) {
    }

    public UIBoxHelper() {
    }

    public void unbindService(final QabelBoxApplication app) {

        Intent serviceIntent = new Intent(app.getApplicationContext(), LocalQabelService.class);
        finished = false;
        app.stopService(serviceIntent);
    }

    public void bindService(final QabelBoxApplication app) {

        Intent serviceIntent = new Intent(app.getApplicationContext(), LocalQabelService.class);
        finished = false;
        //app.stopService(serviceIntent);
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

    public boolean uploadFile(BoxVolume boxVolume, String name, byte[] data, String path) {
        try {

            String folderId = boxVolume.getDocumentId(path);
            Uri uploadUri = DocumentsContract.buildDocumentUri(
                    BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, folderId + name);
            Context self = QabelBoxApplication.getInstance().getApplicationContext();

            OutputStream upload = self.getContentResolver().openOutputStream(uploadUri, "w");
            if (upload == null) {
                return false;
            }
            upload.write(data);
            upload.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Upload failed", e);
        }
        return false;

    }

    public Identity addIdentityWithoutVolume(final String identityName) {

        Identity identity = createIdentity(identityName);
        mService.addIdentity(identity);
        Log.d(TAG, "identity added " + identity.getAlias() + " " + identity.getEcPublicKey().getReadableKeyIdentifier());
        mService.setActiveIdentity(identity);
        return identity;
    }

    public Identity addIdentity(final String identityName) {

        Identity identity = createIdentity(identityName);
        mService.addIdentity(identity);
        Log.d(TAG, "identity added " + identity.getAlias() + " " + identity.getEcPublicKey().getReadableKeyIdentifier());
        mService.setActiveIdentity(identity);

        try {
            initBoxVolume(identity);

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

    @NonNull
    public Identity createIdentity(String identityName) {
        URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
        DropServer dropServer = new DropServer(uri, "", true);
        DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator(2 * 8);
        DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
        Collection<DropURL> dropURLs = new ArrayList<>();
        dropURLs.add(dropURL);

        Identity identity = new Identity(identityName, dropURLs, new QblECKeyPair());
        identity.getPrefixes().add(TestConstants.PREFIX);
        return identity;
    }

    private void initBoxVolume(Identity activeIdentity) throws QblStorageException {

        provider.setLocalService(mService);
        mBoxVolume = provider.getVolumeForRoot(
                activeIdentity.getEcPublicKey().getReadableKeyIdentifier(),
                VolumeFileTransferHelper.getPrefixFromIdentity(activeIdentity));
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

    /**
     * update drawable file
     *
     * @param filename filename to store in box
     * @param format   format type
     * @param id       resource id
     */
    public void uploadDrawableFile(BoxVolume boxVolume, String filename, Bitmap.CompressFormat format, int id) {

        Bitmap bitmap = BitmapFactory.decodeResource(QabelBoxApplication.getInstance().getResources(), id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(100 * 1024);
        bitmap.compress(format, 100, baos);
        byte[] data = new byte[baos.size()];
        System.arraycopy(baos.toByteArray(), 0, data, 0, baos.size());
        uploadFile(boxVolume, filename, data, "");
    }

    /**
     * create new token if needed
     *
     * @param forceCreated set to true if create new forced
     */
    public void createTokenIfNeeded(boolean forceCreated) {
        Context applicationContext = QabelBoxApplication.getInstance().getApplicationContext();
        AppPreference prefs = new AppPreference(applicationContext);
        if (forceCreated && prefs.getToken() == null) {
            prefs.setToken(new RealTokerGetter().getToken(applicationContext));
        } else {
            prefs.setToken(TestConstants.TOKEN);
            prefs.setAccountName("testUser");
        }
    }

    public void removeAllIdentities() {
        mService.deleteContactsAndIdentities();
    }

    public LocalQabelService getService() {
        return mService;
    }

}
