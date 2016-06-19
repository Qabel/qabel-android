package de.qabel.qabelbox;

public class QblBroadcastConstants {

    private static final String PREFIX = "de.qabel.";
    public static final String STATUS_CODE_PARAM = PREFIX + "statusCode";

    public class Storage {
        public static final String BOX_VOLUMES_CHANGES = PREFIX + "boxVolumesChanged";
        public static final String BOX_CHANGED = PREFIX + "boxChanged";
        public static final String BOX_UPLOAD_CHANGED = PREFIX + "boxUploadChanged";
    }

    public class Account {
        public static final String ACCOUNT_CHANGED = PREFIX + "accountChanged";
    }

    public static class Contacts {
        public static final String CONTACTS_CHANGED = PREFIX + "accountChanged";

    }
}
