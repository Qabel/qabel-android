package de.qabel;

public interface QabelContentProviderConstants {

    String CONTENT_AUTHORITY = "de.qabel.android.services.QabelContentProvider";
    String CONTENT_CONTACTS = "contacts";
    String CONTENT_IDENTITIES = "identities";

    String[] CONTACT_COLUMN_NAMES = new String[]{"name", "owner_id", "id"};
    String[] IDENTITIES_COLUMN_NAMES = new String[]{"name", "id"};
}
