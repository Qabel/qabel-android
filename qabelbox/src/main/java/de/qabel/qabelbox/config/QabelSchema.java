package de.qabel.qabelbox.config;

import de.qabel.qabelbox.helper.FileHelper;

public class QabelSchema {

    public static final int TYPE_EXPORT_ONE = 1;
    public static final int TYPE_EXPORT_ALL = 2;

    public static final String FILE_SUFFIX_CONTACT = "qco";
    public static final String FILE_SUFFIX_IDENTITY = "qid";

    public static final String FILE_PREFIX_CONTACT = "contact-";
    public static final String FILE_PREFIX_CONTACTS = "contacts";
    public static final String FILE_PREFIX_IDENTITY = "identity-";


    public static String createContactFilename(String contactAlias) {
        return QabelSchema.FILE_PREFIX_CONTACT + FileHelper.processFilename(contactAlias) +
                "." + QabelSchema.FILE_SUFFIX_CONTACT;
    }

    public static String createExportContactsFileName() {
        return FILE_PREFIX_CONTACTS + "." + FILE_SUFFIX_CONTACT;
    }

}
