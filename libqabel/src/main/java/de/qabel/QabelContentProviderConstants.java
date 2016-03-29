package de.qabel;


// TODO: Move this to a QabelContract class/interface
public interface QabelContentProviderConstants {

	String CONTENT_CONTACTS = "contacts";
	String CONTENT_IDENTITIES = "identities";

    String[] CONTACT_COLUMN_NAMES = new String[]{"name", "owner_id", "id"};
    String[] IDENTITIES_COLUMN_NAMES = new String[]{"name", "id"};
}
