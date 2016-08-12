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
        public static final String CONTACTS_CHANGED = PREFIX + "contacts.updated";
    }

    public static class Chat {
        public static final String NOTIFY_NEW_MESSAGES = PREFIX + "chat.notification";
        public static final String MESSAGE_STATE_CHANGED = PREFIX + "chat.state_change";

        public static class Service {
            public static final String MESSAGES_UPDATED = PREFIX + "chat.service.updated";
            public static final String NOTIFY = PREFIX + "chat.service.notify";
            public static final String MARK_READ = PREFIX + "chat.service.markRead";
            public static final String ADD_CONTACT = PREFIX + "chat.service.addContact";
            public static final String IGNORE_CONTACT = PREFIX + "chat.service.ignoreContact";
        }
    }
}
