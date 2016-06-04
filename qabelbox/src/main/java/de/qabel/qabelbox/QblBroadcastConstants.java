package de.qabel.qabelbox;

public class QblBroadcastConstants {

    private static final String PREFIX = "de.qabel.";

    public class Storage {
        public static final String BOX_VOLUMES_CHANGES = PREFIX + "boxVolumesChanged";
        public static final String BOX_UPLOAD_CHANGED = PREFIX + "boxUploadChanged";
    }

    public class Account {
        public static final String ACCOUNT_CHANGED = PREFIX + "accountChanged";
    }

}
