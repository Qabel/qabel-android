package de.qabel.qabelbox.providers;

import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.exceptions.QblStorageNotFound;
import de.qabel.core.storage.BoxFile;
import de.qabel.core.storage.BoxFolder;
import de.qabel.core.storage.BoxNavigation;
import de.qabel.core.storage.BoxVolume;
import de.qabel.core.storage.TransferManager;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;


public class BoxProvider extends DocumentsProvider {

    private static final String TAG = "BoxProvider";

    public static final String[] DEFAULT_ROOT_PROJECTION = new
            String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_MIME_TYPES,
                    Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
                    Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID,};

    public static final String[] DEFAULT_DOCUMENT_PROJECTION = new
            String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS, Document.COLUMN_SIZE,};


    public static final String AUTHORITY = "de.qabel.qabelbox.providers.documents";
    public static final String PATH_SEP= "/";
    public static final String DOCID_SEPARATOR = "::::";

    public static final String PUB_KEY = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
    public static final String BUCKET = "qabel";
    public static final String PREFIX = "boxtest";
    public static final String PRIVATE_KEY = "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a";
    public static final byte[] DEVICE_ID = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06};

    DocumentIdParser mDocumentIdParser;
    private ThreadPoolExecutor mThreadPoolExecutor;

    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    TransferUtility transferUtility;
    AmazonS3Client amazonS3Client;
    AWSCredentials awsCredentials;

    @Override
    public boolean onCreate() {
        mDocumentIdParser = new DocumentIdParser();

        mThreadPoolExecutor = new ThreadPoolExecutor(
                2,
                2,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingDeque<Runnable>());

        awsCredentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return getContext().getResources().getString(R.string.aws_user);
            }

            @Override
            public String getAWSSecretKey() {
                return getContext().getResources().getString(R.string.aws_password);
            }
        };
        amazonS3Client = new AmazonS3Client(awsCredentials);
        QabelBoxApplication.boxProvider = this;
        return true;
    }

    private void setUpTransferUtility() {
        if (transferUtility == null) {
            transferUtility = new TransferUtility(
                    amazonS3Client,
                    getContext().getApplicationContext());
        }
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        String[] netProjection = reduceProjection(projection, DEFAULT_ROOT_PROJECTION);

        MatrixCursor result = new MatrixCursor(netProjection);
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID,
                mDocumentIdParser.buildId(PUB_KEY,
                        BUCKET, PREFIX, null));
        row.add(Root.COLUMN_DOCUMENT_ID,
                mDocumentIdParser.buildId(PUB_KEY,
                        BUCKET, PREFIX, "/"));
        row.add(Root.COLUMN_ICON, R.drawable.qabel_logo);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE);
        row.add(Root.COLUMN_TITLE, "Qabel Box Test2");
        row.add(Root.COLUMN_SUMMARY, "Foobar");
        return result;
    }

    private String[] reduceProjection(String[] projection, String[] supportedProjection) {
        if (projection == null) {
            return supportedProjection;
        }
        HashSet<String> supported = new HashSet<>(Arrays.asList(supportedProjection));
        ArrayList<String> result = new ArrayList<>();
        for (String column: projection) {
            if (supported.contains(column)) {
                result.add(column);
            }
        }
        return result.toArray(projection);
    }

    public BoxVolume getVolumeForRoot(String identity, String bucket, String prefix) {
        if (bucket == null) {
            bucket = BUCKET;
        }
        if (prefix == null) {
            prefix = PREFIX;
        }
        QblECKeyPair testKey = new QblECKeyPair(Hex.decode(PRIVATE_KEY));

        setUpTransferUtility();
        return new BoxVolume(transferUtility, awsCredentials, testKey, bucket, prefix,
                    DEVICE_ID, getContext().getCacheDir());
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        Log.d(TAG, "Query Document: " + documentId);
        MatrixCursor cursor = new MatrixCursor(
                reduceProjection(projection, DEFAULT_DOCUMENT_PROJECTION));

        String filePath = mDocumentIdParser.getFilePath(documentId);

        BoxVolume volume = getVolumeForId(documentId);

        if (filePath.equals(PATH_SEP)) {
            // root id
            insertRootDoc(cursor, documentId);
            return cursor;
        }

        try {
            List<String> strings = mDocumentIdParser.splitPath(mDocumentIdParser.getFilePath(documentId));
            String basename = strings.remove(strings.size()-1);
            BoxNavigation navigation =
                    traverseToFolder(volume, strings);
            Log.d(TAG, "Inserting basename " + basename);
            insertFileByName(cursor, navigation, documentId, basename);
        } catch (QblStorageException e) {
            Log.i(TAG, "Could not find document " + documentId, e);
            throw new FileNotFoundException("Failed navigating the volume");
        }
        return cursor;
    }

    private BoxVolume getVolumeForId(String documentId) throws FileNotFoundException {
        return getVolumeForRoot(
                    mDocumentIdParser.getIdentity(documentId),
                    mDocumentIdParser.getBucket(documentId),
                    mDocumentIdParser.getPrefix(documentId));
    }

    void insertFileByName(MatrixCursor cursor, BoxNavigation navigation,
                          String documentId, String basename) throws QblStorageException {
        for (BoxFolder folder: navigation.listFolders()) {
            Log.d(TAG, "Checking folder:" + folder.name);
            if (basename.equals(folder.name)) {
                insertFolder(cursor, documentId, folder);
                return;
            }
        }
        for (BoxFile file: navigation.listFiles()) {
            Log.d(TAG, "Checking file:" + file.name);
            if (basename.equals(file.name)) {
                insertFile(cursor, documentId, file);
                return;
            }
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
        MatrixCursor cursor = new MatrixCursor(
                reduceProjection(projection, DEFAULT_DOCUMENT_PROJECTION));

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
        return cursor;
    }

    private void insertFolderListing(MatrixCursor cursor, BoxNavigation navigation, String parentDocumentId) throws QblStorageException {
        for (BoxFolder folder: navigation.listFolders()) {
            insertFolder(cursor, parentDocumentId + folder.name + PATH_SEP, folder);
        }
        for (BoxFile file: navigation.listFiles()) {
            insertFile(cursor, parentDocumentId + file.name, file);
        }
    }

    private void insertFile(MatrixCursor cursor, String documentId, BoxFile file) {
        final MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        row.add(Document.COLUMN_DISPLAY_NAME, file.name);
        row.add(Document.COLUMN_SUMMARY, null);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_WRITE);
        row.add(Document.COLUMN_MIME_TYPE,
                URLConnection.guessContentTypeFromName(file.name));
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
        PARTS: for (String part: filePath) {
            if (part.equals("")) {
                continue;
            }
            for (BoxFolder folder: navigation.listFolders()) {
                Log.i(TAG, "Part: " + part + " Folder: " + folder.name);
                if (part.equals(folder.name)) {
                    navigation = navigation.navigate(folder);
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

        if (isWrite) {
            final int id = 1;
            final NotificationManager mNotifyManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getContext());
            mBuilder.setContentTitle("Uploading " + mDocumentIdParser.getBaseName(documentId))
                    .setContentText("Upload in progress")
                    .setSmallIcon(R.drawable.notification_template_icon_bg);
            mBuilder.setProgress(100, 0, false);
            mNotifyManager.notify(id, mBuilder.build());

            // Attach a close listener if the document is opened in write mode.
            try {
                Handler handler = new Handler(getContext().getMainLooper());
                final File tmp = File.createTempFile("upload", "", getContext().getExternalCacheDir());
                return ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.parseMode(mode), handler,
                        new ParcelFileDescriptor.OnCloseListener() {
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
                                        uploadFile(documentId, tmp, new TransferManager.BoxTransferListener() {
                                            @Override
                                            public void onProgressChanged(long bytesCurrent, long bytesTotal) {
                                                mBuilder.setProgress(100, (int) (100 * bytesCurrent / bytesTotal), false);
                                                mNotifyManager.notify(id, mBuilder.build());
                                            }

                                            @Override
                                            public void onFinished() {
                                                String filename;
                                                try {
                                                    filename = mDocumentIdParser.getBaseName(documentId);
                                                } catch (FileNotFoundException e) {
                                                    filename = documentId;
                                                }
                                                mBuilder.setContentTitle("Uploading " + filename)
                                                        .setContentText("Upload complete")
                                                        .setSmallIcon(R.drawable.notification_template_icon_bg);
                                                mBuilder.setProgress(100, 100, false);
                                                mNotifyManager.notify(id, mBuilder.build());
                                            }
                                        });
                                        return documentId;
                                    }
                                }.execute();
                            }

                        });
            } catch (IOException e) {
                throw new FileNotFoundException();
            }
        }
        else {
            return downloadFile(documentId, mode, signal);
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
            Log.i(TAG, "Starting upload");
            navigation.upload(basename, new FileInputStream(tmp), boxTransferListener);
            navigation.commit();
        } catch (FileNotFoundException | QblStorageException e1) {
            Log.e(TAG, "Upload failed", e1);
        }
    }

    private ParcelFileDescriptor downloadFile(final String documentId, final String mode, final CancellationSignal signal) throws FileNotFoundException {
        final Future<ParcelFileDescriptor> future
                = mThreadPoolExecutor.submit(new Callable<ParcelFileDescriptor>() {

            @Override
            public ParcelFileDescriptor call() throws Exception {

                File f = getFile(signal, documentId);

                // return the file to the client.
                return makeParcelFileDescriptor(f, mode);
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

    private ParcelFileDescriptor makeParcelFileDescriptor(File f, String mode)
            throws FileNotFoundException {
        final int accessMode = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(f, accessMode);
    }

    private File getFile(CancellationSignal signal, final String documentId)
            throws IOException, QblStorageException {
        final int id = 2;
        final NotificationManager mNotifyManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getContext());
        mBuilder.setContentTitle("Downloading " + mDocumentIdParser.getBaseName(documentId))
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.notification_template_icon_bg);
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
                String filename;
                try {
                    filename = mDocumentIdParser.getBaseName(documentId);
                } catch (FileNotFoundException e) {
                    filename = documentId;
                }
                mBuilder.setContentTitle("Downloading " + filename)
                        .setContentText("Download complete")
                        .setSmallIcon(R.drawable.notification_template_icon_bg);
                mBuilder.setProgress(100, 100, false);
                mNotifyManager.notify(id, mBuilder.build());
            }
        });
        File out = new File(getContext().getExternalCacheDir(), basename);
        FileOutputStream fileOutputStream = new FileOutputStream(out);
        IOUtils.copy(inputStream, fileOutputStream);
        inputStream.close();
        fileOutputStream.close();
        return out;
    }

    private BoxFile findFileinList(String basename, BoxNavigation navigation)
            throws QblStorageException, FileNotFoundException {
        for (BoxFile file: navigation.listFiles()) {
            if (file.name.equals(basename)) {
                return file;
            }
        }
        throw new FileNotFoundException();
    }

    @Override
    public String createDocument (String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
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
            String basename = splitPath.remove(splitPath.size()-1);
            BoxNavigation navigation = traverseToFolder(volume, splitPath);

            for (BoxFile file: navigation.listFiles()) {
                if (file.name.equals(basename)) {
                    navigation.delete(file);
                    navigation.commit();
                    return;
                }
            }
            for (BoxFolder folder: navigation.listFolders()) {
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
            String basename = splitPath.remove(splitPath.size()-1);
            BoxNavigation navigation = traverseToFolder(volume, splitPath);
            splitPath.add(PATH_SEP + displayName);
            String newPath = StringUtils.join(splitPath, "");
            String renamedId = mDocumentIdParser.buildId(
                    mDocumentIdParser.getIdentity(documentId),
                    mDocumentIdParser.getBucket(documentId),
                    mDocumentIdParser.getPrefix(documentId),
                    newPath);

            for (BoxFile file: navigation.listFiles()) {
                if (file.name.equals(basename)) {
                    navigation.rename(file, displayName);
                    navigation.commit();
                    return renamedId;
                }
            }
            for (BoxFolder folder: navigation.listFolders()) {
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
}
