package de.qabel.qabelbox.providers;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.provider.MediaStore.Video.Media;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.VolumeFileTransferHelper;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNotFound;
import de.qabel.qabelbox.services.LocalBroadcastConstants;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxExternalFile;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxUploadingFile;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.storage.TransferManager;

public class BoxProvider extends DocumentsProvider {

    private static final String TAG = "BoxProvider";

    public static final String[] DEFAULT_ROOT_PROJECTION = new
            String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID,};

    public static final String[] DEFAULT_DOCUMENT_PROJECTION = new
            String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Media.DATA};

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".providers.documents";
    public static final String PATH_SEP = "/";
    public static final String DOCID_SEPARATOR = "::::";
    public static final String PREFIX = "test";

    DocumentIdParser mDocumentIdParser;
    private ThreadPoolExecutor mThreadPoolExecutor;

    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private Map<String, BoxCursor> folderContentCache;
    private String currentFolder;
    protected LocalQabelService mService;

    @Override
    public boolean onCreate() {

        final Context context = getContext();
        if (context == null) {
            Log.e(TAG, "No context available in BoxProvider, exiting");
            return false;
        }

        bindToService(context);

        mDocumentIdParser = new DocumentIdParser();

        mThreadPoolExecutor = new ThreadPoolExecutor(
                2,
                2,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingDeque<Runnable>());

        QabelBoxApplication.boxProvider = this;

        folderContentCache = new HashMap<>();
        return true;
    }

    void bindToService(Context context) {

        Intent intent = new Intent(context, LocalQabelService.class);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                mService = binder.getService();
                notifyRootsUpdated();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

                mService = null;
            }
        }, Context.BIND_AUTO_CREATE);
    }

    /**
     * Notify the system that the roots have changed
     * This happens if identities or prefixes changed.
     */
    public void notifyRootsUpdated() {

        getContext().getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null);
    }

    /**
     * Used to temporary inject the service if it is not ready yet
     *
     * @param service
     */
    public void setLocalService(LocalQabelService service) {

        if (mService == null) {
            mService = service;
        }
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {

        String[] netProjection = reduceProjection(projection, DEFAULT_ROOT_PROJECTION);

        MatrixCursor result = new MatrixCursor(netProjection);
        if (mService == null) {
            return result;
        }
        Identities identities = mService.getIdentities();
        for (Identity identity : identities.getIdentities()) {
            final MatrixCursor.RowBuilder row = result.newRow();
            String pub_key = identity.getEcPublicKey().getReadableKeyIdentifier();
            String prefix;
            try {
                prefix = identity.getPrefixes().get(0);
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Could not find a prefix in identity " + pub_key);
                continue;
            }
            row.add(Root.COLUMN_ROOT_ID,
                    mDocumentIdParser.buildId(pub_key, prefix, null));
            row.add(Root.COLUMN_DOCUMENT_ID,
                    mDocumentIdParser.buildId(pub_key, prefix, "/"));
            row.add(Root.COLUMN_ICON, R.drawable.qabel_logo);
            row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE);
            row.add(Root.COLUMN_TITLE, "Qabel Box");
            row.add(Root.COLUMN_SUMMARY, identity.getAlias());
        }

        return result;
    }

    private String[] reduceProjection(String[] projection, String[] supportedProjection) {

        if (projection == null) {
            return supportedProjection;
        }
        HashSet<String> supported = new HashSet<>(Arrays.asList(supportedProjection));
        ArrayList<String> result = new ArrayList<>();
        for (String column : projection) {
            if (supported.contains(column)) {
                result.add(column);
            } else {
                Log.w(TAG, "Requested cursor field don't supported '" + column + "'");
            }
        }
        if (result.size() == 0) {
            Log.e(TAG, "Cursors contain no fields after reduceProjection. Add fallback field");
            //add fallback if no field supported. this avoid crashes on different third party apps
            result.add(Document.COLUMN_DOCUMENT_ID);
        }

        return result.toArray(projection);
    }

    public BoxVolume getVolumeForRoot(String identity, String prefix) {

        if (prefix == null) {
            throw new RuntimeException("No prefix supplied");
        }
        Identity retrievedIdentity = mService.getIdentities().getByKeyIdentifier(identity);
        if (retrievedIdentity == null) {
            throw new RuntimeException("Identity " + identity + "is unknown!");
        }
        QblECKeyPair key = retrievedIdentity.getPrimaryKeyPair();

        return new BoxVolume(key, prefix,
                mService.getDeviceID(), getContext());
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {

        MatrixCursor cursor = createCursor(projection, false);
        String logInfos = shrinkDocumentId(documentId);
        if (projection != null) {
            logInfos += " projSize=" + projection.length;
        } else {
            logInfos += " projection=null. All fields used";
        }
        Log.v(TAG, "QueryDocument " + logInfos);
        String filePath = mDocumentIdParser.getFilePath(documentId);

        BoxVolume volume = getVolumeForId(documentId);

        if (filePath.equals(PATH_SEP)) {
            // root id
            insertRootDoc(cursor, documentId);
            return cursor;
        }

        try {
            List<String> strings = mDocumentIdParser.splitPath(mDocumentIdParser.getFilePath(documentId));
            String basename = strings.remove(strings.size() - 1);
            BoxNavigation navigation =
                    traverseToFolder(volume, strings);
            Log.d(TAG, "Inserting basename " + basename);
            insertFileByName(cursor, navigation, documentId, basename);
        } catch (QblStorageException e) {
            Log.i(TAG, "Could not find document " + documentId, e);
            throw new FileNotFoundException("Failed navigating the volume");
        }

        Log.v(TAG, "quere roots result, cursorCount=" + cursor.getCount() + " cursorColumn=" + cursor.getColumnCount());
        return cursor;
    }

    private String shrinkDocumentId(String documentId) {

        if (documentId == null) {
            return null;
        }
        String[] elements = documentId.split("/");
        return elements[elements.length - 1];
    }

    private BoxVolume getVolumeForId(String documentId) throws FileNotFoundException {

        return getVolumeForRoot(
                mDocumentIdParser.getIdentity(documentId),
                mDocumentIdParser.getPrefix(documentId));
    }

    void insertFileByName(MatrixCursor cursor, BoxNavigation navigation,
                          String documentId, String basename) throws QblStorageException {

        for (BoxFolder folder : navigation.listFolders()) {
            Log.d(TAG, "Checking folder:" + folder.name);
            if (basename.equals(folder.name)) {
                insertFolder(cursor, documentId, folder);
                return;
            }
        }
        for (BoxFile file : navigation.listFiles()) {
            Log.d(TAG, "Checking file:" + file.name);
            if (basename.equals(file.name)) {
                insertFile(cursor, documentId, file);
                return;
            }
        }
        BoxObject external = navigation.getExternal(basename);
        if (external != null) {
            insertFile(cursor, documentId, external);
            return;
        }
        throw new QblStorageNotFound("File not found");
    }

    void insertRootDoc(MatrixCursor cursor, String documentId) {

        final MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        row.add(Document.COLUMN_DISPLAY_NAME, "Root");
        row.add(Document.COLUMN_SUMMARY, null);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {

        Log.d(TAG, "Query Child Documents: " + parentDocumentId);
        BoxCursor cursor = folderContentCache.get(parentDocumentId);
        boolean cacheHit = (cursor != null);
        if (parentDocumentId.equals(currentFolder) && cacheHit) {
            // best case: we are still in the same folder and we got a cache hit
            Log.d(TAG, "Up to date cached data found");
            cursor.setExtraLoading(false);
            return cursor;
        }
        if (cacheHit) {
            // we found it in the cache, but since we changed the folder, we refresh anyway
            cursor.setExtraLoading(true);
        } else {
            Log.d(TAG, "Serving empty listing and refreshing");
            cursor = createCursor(projection, true);
        }
        currentFolder = parentDocumentId;
        asyncChildDocuments(parentDocumentId, projection, cursor);
        return cursor;
    }

    /**
     * Create and fill a new MatrixCursor
     * <p/>
     * The cursor can be modified to show a loading and/or an error message.
     *
     * @param parentDocumentId
     * @param projection
     * @return Fully initialized cursor with the directory listing as rows
     * @throws FileNotFoundException
     */
    private BoxCursor createBoxCursor(String parentDocumentId, String[] projection) throws FileNotFoundException {

        Log.v(TAG, "createBoxCursor");
        BoxCursor cursor = createCursor(projection, false);

        BoxVolume volume = getVolumeForId(parentDocumentId);
        try {
            BoxNavigation navigation =
                    traverseToFolder(volume, mDocumentIdParser.splitPath(
                            mDocumentIdParser.getFilePath(parentDocumentId)));
            insertFolderListing(cursor, navigation, parentDocumentId);
        } catch (QblStorageException e) {
            Log.e(TAG, "Could not navigate", e);
            throw new FileNotFoundException("Failed navigating the volume");
        }
        folderContentCache.put(parentDocumentId, cursor);
        return cursor;
    }

    /**
     * Query the directory listing, store the cursor in the folderContentCache and
     * notify the original cursor of the update.
     *
     * @param parentDocumentId
     * @param projection
     * @param result           Original cursor
     */
    private void asyncChildDocuments(final String parentDocumentId, final String[] projection,
                                     BoxCursor result) {

        Log.v(TAG, "asyncChildDocuments");
        final Uri uri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, parentDocumentId);
        // tell the original cursor how he gets notified
        result.setNotificationUri(getContext().getContentResolver(), uri);

        // create a new cursor and store it
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    createBoxCursor(parentDocumentId, projection);
                } catch (FileNotFoundException e) {
                    BoxCursor cursor = createCursor(projection, false);
                    cursor.setError(getContext().getString(R.string.folderListingUpdateError));
                    folderContentCache.put(parentDocumentId, cursor);
                }
                getContext().getContentResolver().notifyChange(uri, null);
            }
        });
    }

    @NonNull
    private BoxCursor createCursor(String[] projection, final boolean extraLoading) {

        String[] reduced = reduceProjection(projection, DEFAULT_DOCUMENT_PROJECTION);
        BoxCursor cursor = new BoxCursor(reduced);
        cursor.setExtraLoading(extraLoading);
        return cursor;
    }

    private void insertFolderListing(MatrixCursor cursor, BoxNavigation navigation, String parentDocumentId) throws QblStorageException {

        for (BoxFolder folder : navigation.listFolders()) {
            insertFolder(cursor, parentDocumentId + folder.name + PATH_SEP, folder);
        }
        for (BoxFile file : navigation.listFiles()) {
            insertFile(cursor, parentDocumentId + file.name, file);
        }
        for (BoxObject file : navigation.listExternalNames()) {
            insertFile(cursor, parentDocumentId + file.name, file);
        }
    }

    private void insertFile(MatrixCursor cursor, String documentId, BoxObject file) {

        final MatrixCursor.RowBuilder row = cursor.newRow();
        String mimeType = URLConnection.guessContentTypeFromName(file.name);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        row.add(Document.COLUMN_DISPLAY_NAME, file.name);
        row.add(Document.COLUMN_SUMMARY, null);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_WRITE);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Media.DATA, documentId);
    }

    private void insertFolder(MatrixCursor cursor, String documentId, BoxFolder folder) {

        final MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        row.add(Document.COLUMN_DISPLAY_NAME, folder.name);
        row.add(Document.COLUMN_SUMMARY, null);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
    }

    BoxNavigation traverseToFolder(BoxVolume volume, List<String> filePath) throws QblStorageException {

        Log.d(TAG, "Traversing to " + filePath.toString());
        BoxNavigation navigation = volume.navigate();
        PARTS:
        for (String part : filePath) {
            if (part.equals("")) {
                continue;
            }
            for (BoxFolder folder : navigation.listFolders()) {
                Log.i(TAG, "Part: " + part + " Folder: " + folder.name);
                if (part.equals(folder.name)) {
                    navigation.navigate(folder);
                    continue PARTS;
                }
            }
            throw new QblStorageNotFound("Folder not found, giving up at " + part);
        }
        return navigation;
    }

    @Override
    public ParcelFileDescriptor openDocument(final String documentId,
                                             final String mode, final CancellationSignal signal)
            throws FileNotFoundException {

        Log.d(TAG, "Open document: " + documentId);
        final boolean isWrite = (mode.indexOf('w') != -1);
        final boolean isRead = (mode.indexOf('r') != -1);

        if (isWrite) {
            final BoxUploadingFile boxUploadingFile = mService.addPendingUpload(documentId, null);
            // Attach a close listener if the document is opened in write mode.
            try {
                Handler handler = new Handler(getContext().getMainLooper());
                final File tmp;
                if (isRead) {
                    tmp = downloadFile(documentId, mode, signal);
                } else {
                    tmp = File.createTempFile("uploadAndDeleteLocalfile", "", getContext().getExternalCacheDir());
                }
                ParcelFileDescriptor.OnCloseListener onCloseListener = new ParcelFileDescriptor.OnCloseListener() {
                    @Override
                    public void onClose(IOException e) {
                        // Update the file with the cloud server.  The client is done writing.
                        Log.i(TAG, "A file with id " + documentId + " has been closed!  Time to " +
                                "update the server.");
                        if (e != null) {
                            Log.e(TAG, "IOException in onClose", e);
                            return;
                        }
                        // in another thread!
                        new AsyncTask<Void, Void, String>() {
                            @Override
                            protected String doInBackground(Void... params) {

                                uploadFile(documentId, tmp, mService.getUploadTransferListener(boxUploadingFile));
                                return documentId;
                            }
                        }.execute();
                    }
                };
                return ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.parseMode(mode), handler,
                        onCloseListener);
            } catch (IOException e) {
                throw new FileNotFoundException();
            }
        } else {
            File tmp = downloadFile(documentId, mode, signal);
            final int accessMode = ParcelFileDescriptor.parseMode(mode);
            return ParcelFileDescriptor.open(tmp, accessMode);
        }
    }

    private void uploadFile(String documentId, File tmp, TransferManager.BoxTransferListener boxTransferListener) {

        try {
            BoxVolume volume = getVolumeForId(documentId);
            List<String> splitPath = mDocumentIdParser.splitPath(
                    mDocumentIdParser.getFilePath(documentId));
            String basename = splitPath.remove(splitPath.size() - 1);
            Log.i(TAG, "Navigating to folder");
            BoxNavigation navigation = traverseToFolder(volume, splitPath);
            Log.i(TAG, "Starting uploadAndDeleteLocalfile");
            BoxFile boxFile = navigation.upload(basename, new FileInputStream(tmp), boxTransferListener);
            navigation.commit();
            Bundle extras = new Bundle();
            extras.putParcelable(LocalBroadcastConstants.EXTRA_FILE, boxFile);
            mService.removePendingUpload(documentId, LocalBroadcastConstants.UPLOAD_STATUS_FINISHED, extras);
        } catch (FileNotFoundException | QblStorageException e1) {
            Log.e(TAG, "Upload failed", e1);
            try {
                mService.removePendingUpload(documentId, LocalBroadcastConstants.UPLOAD_STATUS_FAILED, null);
            } catch (FileNotFoundException e) {
                //Should not be possible
                Log.e(TAG, "Removing failed upload failed", e);
            }
        }
    }

    private File downloadFile(final String documentId, final String mode, final CancellationSignal signal) throws FileNotFoundException {

        final Future<File> future
                = mThreadPoolExecutor.submit(new Callable<File>() {

            @Override
            public File call() throws Exception {

                return getFile(signal, documentId);
            }
        });
        if (signal != null) {
            signal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                @Override
                public void onCancel() {

                    Log.d(TAG, "openDocument cancelling");
                    future.cancel(true);
                }
            });
        }

        try {
            return future.get();
        } catch (InterruptedException e) {
            Log.d(TAG, "openDocument cancelled download");
            throw new FileNotFoundException();
        } catch (ExecutionException e) {
            Log.d(TAG, "Execution error", e);
            throw new FileNotFoundException();
        }
    }

    private File getFile(CancellationSignal signal, final String documentId)
            throws IOException, QblStorageException {

        final int id = 2;
        //@todo notification handling into separate place
        final NotificationManager mNotifyManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getContext());
        mBuilder.setContentTitle("Downloading " + mDocumentIdParser.getBaseName(documentId))
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.qabel_logo);
        mBuilder.setProgress(100, 0, false);
        mNotifyManager.notify(id, mBuilder.build());

        String path = mDocumentIdParser.getFilePath(documentId);
        List<String> strings = mDocumentIdParser.splitPath(path);
        String basename = strings.remove(strings.size() - 1);
        BoxVolume volume = getVolumeForId(documentId);

        BoxNavigation navigation = traverseToFolder(volume, strings);
        BoxFile file = findFileinList(basename, navigation);
        InputStream inputStream = navigation.download(file, new TransferManager.BoxTransferListener() {

            @Override
            public void onProgressChanged(long bytesCurrent, long bytesTotal) {

                mBuilder.setProgress(100, (int) (100 * bytesCurrent / bytesTotal), false);
                mNotifyManager.notify(id, mBuilder.build());
            }

            @Override
            public void onFinished() {

            }
        });
        String filename;
        try {
            filename = mDocumentIdParser.getBaseName(documentId);
        } catch (FileNotFoundException e) {
            filename = documentId;
        }
        mBuilder.setContentTitle("Downloading " + filename)
                .setContentText("Download complete")
                .setSmallIcon(R.drawable.qabel_logo);
        mBuilder.setProgress(100, 100, false);
        mNotifyManager.notify(id, mBuilder.build());
        File out = new File(getContext().getExternalCacheDir(), basename);
        FileOutputStream fileOutputStream = new FileOutputStream(out);
        IOUtils.copy(inputStream, fileOutputStream);
        inputStream.close();
        fileOutputStream.close();
        return out;
    }

    private BoxFile findFileinList(String basename, BoxNavigation navigation)
            throws QblStorageException, FileNotFoundException {

        for (BoxFile file : navigation.listFiles()) {
            Log.d(TAG, "find file: " + file.name);
            if (file.name.equals(basename)) {
                return file;
            }
        }
        for (BoxObject file : navigation.listExternals()) {
            Log.d(TAG, "find file: " + file.name);
            if (file instanceof BoxExternalFile) {
                if (file.name.equals(basename)) {
                    return (BoxExternalFile)file;
                }
            }
        }
        throw new FileNotFoundException("can't find file in BoxNavigation: " + basename);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {

        Log.d(TAG, "createDocument: " + parentDocumentId + "; " + mimeType + "; " + displayName);

        String parentPath = mDocumentIdParser.getFilePath(parentDocumentId);
        BoxVolume volume = getVolumeForId(parentDocumentId);

        try {

            BoxNavigation navigation = traverseToFolder(volume, mDocumentIdParser.splitPath(parentPath));

            if (mimeType.equals(Document.MIME_TYPE_DIR)) {
                navigation.createFolder(displayName);
            } else {
                navigation.upload(displayName, new ByteArrayInputStream(new byte[0]), null);
            }
            navigation.commit();

            return parentDocumentId + displayName;
        } catch (QblStorageException e) {
            Log.e(TAG, "could not create file", e);
            throw new FileNotFoundException();
        }
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {

        Log.d(TAG, "deleteDocument: " + documentId);

        String path = mDocumentIdParser.getFilePath(documentId);
        BoxVolume volume = getVolumeForId(documentId);

        try {
            List<String> splitPath = mDocumentIdParser.splitPath(path);
            String basename = splitPath.remove(splitPath.size() - 1);
            BoxNavigation navigation = traverseToFolder(volume, splitPath);

            for (BoxFile file : navigation.listFiles()) {
                if (file.name.equals(basename)) {
                    navigation.delete(file);
                    navigation.commit();
                    return;
                }
            }
            for (BoxFolder folder : navigation.listFolders()) {
                if (folder.name.equals(basename)) {
                    navigation.delete(folder);
                    navigation.commit();
                    return;
                }
            }
        } catch (QblStorageException e) {
            Log.e(TAG, "could not create file", e);
            throw new FileNotFoundException();
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {

        Log.d(TAG, "renameDocument: " + documentId + " to " + displayName);

        String path = mDocumentIdParser.getFilePath(documentId);
        BoxVolume volume = getVolumeForId(documentId);

        try {
            List<String> splitPath = mDocumentIdParser.splitPath(path);
            String basename = splitPath.remove(splitPath.size() - 1);
            BoxNavigation navigation = traverseToFolder(volume, splitPath);
            splitPath.add(PATH_SEP + displayName);
            String newPath = StringUtils.join(splitPath, "");
            String renamedId = mDocumentIdParser.buildId(
                    mDocumentIdParser.getIdentity(documentId),
                    mDocumentIdParser.getPrefix(documentId),
                    newPath);

            for (BoxFile file : navigation.listFiles()) {
                if (file.name.equals(basename)) {
                    navigation.rename(file, displayName);
                    navigation.commit();
                    return renamedId;
                }
            }
            for (BoxFolder folder : navigation.listFolders()) {
                if (folder.name.equals(basename)) {
                    navigation.rename(folder, displayName);
                    navigation.commit();
                    return renamedId;
                }
            }
            throw new FileNotFoundException();
        } catch (QblStorageException e) {
            Log.e(TAG, "could not create file", e);
            throw new FileNotFoundException();
        }
    }

    class BoxCursor extends MatrixCursor {

        private boolean extraLoading;
        private String error;

        public BoxCursor(String[] columnNames) {

            super(columnNames);
        }

        public void setExtraLoading(boolean loading) {

            this.extraLoading = loading;
        }

        public Bundle getExtras() {

            Bundle bundle = new Bundle();
            bundle.putBoolean(DocumentsContract.EXTRA_LOADING, extraLoading);
            if (error != null) {
                bundle.putString(DocumentsContract.EXTRA_ERROR, error);
            }
            return bundle;
        }

        public void setError(String error) {

            this.error = error;
        }
    }
}
