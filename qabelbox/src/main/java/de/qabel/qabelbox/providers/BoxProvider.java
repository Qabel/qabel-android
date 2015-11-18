package de.qabel.qabelbox.providers;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import org.spongycastle.util.encoders.Hex;

import java.io.FileNotFoundException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.exceptions.QblStorageNotFound;
import de.qabel.core.storage.BoxFile;
import de.qabel.core.storage.BoxFolder;
import de.qabel.core.storage.BoxNavigation;
import de.qabel.core.storage.BoxVolume;
import de.qabel.qabelbox.R;


public class BoxProvider extends DocumentsProvider {

    private static final String TAG = "BoxProvider";

    private static final String PATH_SEP= "/";

    private static final String[] DEFAULT_ROOT_PROJECTION = new
            String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_MIME_TYPES,
                    Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
                    Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID,};

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new
            String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS, Document.COLUMN_SIZE,};

    DocumentIdParser documentIdParser;
    BoxVolume boxVolume;

    @Override
    public boolean onCreate() {
        documentIdParser = new DocumentIdParser();
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        String[] netProjection = reduceProjection(projection, DEFAULT_ROOT_PROJECTION);

        MatrixCursor result = new MatrixCursor(netProjection);
        String publicKey = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID,
                documentIdParser.buildId(publicKey,
                        "qabel", "boxtest", null));
        row.add(Root.COLUMN_DOCUMENT_ID,
                documentIdParser.buildId(publicKey,
                        "qabel", "boxtest", "/"));
        row.add(Root.COLUMN_ICON, R.drawable.qabel_logo);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_IS_CHILD | Root.FLAG_SUPPORTS_CREATE);
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

    BoxVolume getVolumeForRoot(String identity, String bucket, String rootId) {
        if (boxVolume == null) {
            QblECKeyPair testKey = new QblECKeyPair(Hex.decode(
                    "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"));

            AWSCredentials credentials = new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return getContext().getResources().getString(R.string.aws_user);
                }

                @Override
                public String getAWSSecretKey() {
                    return getContext().getResources().getString(R.string.aws_password);
                }
            };

            AmazonS3Client amazonS3Client = new AmazonS3Client(credentials);
            TransferUtility transferUtility = new TransferUtility(
                    amazonS3Client,
                    getContext().getApplicationContext());
            boxVolume = new BoxVolume(transferUtility, credentials, testKey, "qabel", "boxtest",
                    new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06}, getContext().getCacheDir());
        }
        return boxVolume;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        Log.d(TAG, "Query Document: " + documentId);
        MatrixCursor cursor = new MatrixCursor(
                reduceProjection(projection, DEFAULT_DOCUMENT_PROJECTION));

        String filePath = documentIdParser.getFilePath(documentId);

        BoxVolume volume = getVolumeForRoot(
                documentIdParser.getIdentity(documentId),
                documentIdParser.getBucket(documentId),
                documentIdParser.getPrefix(documentId));

        if (filePath.equals(PATH_SEP)) {
            // root id
            insertRootDoc(cursor, documentId);
            return cursor;
        }

        try {
            List<String> strings = documentIdParser.splitPath(documentIdParser.getFilePath(documentId));
            String basename = strings.remove(strings.size()-1);
            BoxNavigation navigation =
                    traverseToFolder(volume, strings);
            Log.d(TAG, "Inserting basename " + basename);
            insertFileByName(cursor, navigation, documentId, basename);
        } catch (QblStorageException e) {
            Log.e(TAG, "Could not navigate", e);
            throw new FileNotFoundException("Failed navigating the volume");
        }
        return cursor;
    }

    void insertFileByName(MatrixCursor cursor, BoxNavigation navigation,
                          String documentId, String basename) throws QblStorageException {
        for (BoxFolder folder: navigation.listFolders()) {
            if (basename.equals(folder.name)) {
                insertFolder(cursor, documentId, folder);
                return;
            }
        }
        for (BoxFile file: navigation.listFiles()) {
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

        BoxVolume volume = getVolumeForRoot(
                documentIdParser.getIdentity(parentDocumentId),
                documentIdParser.getBucket(parentDocumentId),
                documentIdParser.getPrefix(parentDocumentId));
        try {
            BoxNavigation navigation =
                    traverseToFolder(volume, documentIdParser.splitPath(
                            documentIdParser.getFilePath(parentDocumentId)));
            insertFolderListing(cursor, navigation, parentDocumentId);
        } catch (QblStorageException e) {
            Log.e(TAG, "Could not navigate", e);
            throw new FileNotFoundException("Failed navigating the volume");
        }
        return cursor;
    }

    void insertFolderListing(MatrixCursor cursor, BoxNavigation navigation, String parentDocumentId) throws QblStorageException {
        for (BoxFolder folder: navigation.listFolders()) {
            insertFolder(cursor, parentDocumentId + PATH_SEP + folder.name, folder);
        }
        for (BoxFile file: navigation.listFiles()) {
            insertFile(cursor, parentDocumentId + PATH_SEP + file.name, file);
        }
    }

    private void insertFile(MatrixCursor cursor, String parentDocumentId, BoxFile file) {
        final MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, parentDocumentId + PATH_SEP + file.name);
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
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        return null;
    }

}
