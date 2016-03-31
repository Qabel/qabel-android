package de.qabel.qabelbox.services;

public class LocalBroadcastConstants {
    // BoxProvider uploadAndDeleteLocalfile constants
    public static final String INTENT_UPLOAD_BROADCAST = "uploadBroadcast";
    public static final String EXTRA_UPLOAD_DOCUMENT_ID = "documentID";
    public static final String EXTRA_UPLOAD_STATUS = "uploadStatus";
    public static final String EXTRA_UPLOAD_EXTRA = "uploadExtras";
    public static final int UPLOAD_STATUS_NEW = 1;
    public static final int UPLOAD_STATUS_FINISHED = 2;
    public static final int UPLOAD_STATUS_FAILED = 3;
    public static final String EXTRA_FILE = "extraFile";
}
