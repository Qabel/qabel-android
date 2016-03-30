package de.qabel.qabelbox.config;


import android.support.annotation.NonNull;
import android.util.Log;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

public class ContactExportImport {
    public static final String TAG = "ContactExportImport";

    private static final String KEY_ALIAS = "alias";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PUBLIC_KEY = "public_key";
    private static final String KEY_DROP_URLS = "drop_urls";
    private static final String KEY_CONTACTS = "contacts";


    public static String exportContacts(Contacts contacts) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonContacts = new JSONArray();
        for (Contact contact : contacts.getContacts()) {
            jsonContacts.put(getJSONfromContact(contact));
        }
        jsonObject.put(KEY_CONTACTS, jsonContacts);
        return jsonObject.toString();
    }

    public static String exportContact(Contact contact) {
        return getJSONfromContact(contact).toString();
    }

    private static JSONObject getJSONfromContact(Contact contact) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonDropUrls = new JSONArray();
        try {
            jsonObject.put(KEY_ALIAS, contact.getAlias());
            jsonObject.put(KEY_EMAIL, contact.getEmail());
            jsonObject.put(KEY_PHONE, contact.getPhone());
            jsonObject.put(KEY_PUBLIC_KEY, contact.getKeyIdentifier());
            for (DropURL dropURL : contact.getDropUrls()) {
                jsonDropUrls.put(dropURL);
            }
            jsonObject.put(KEY_DROP_URLS, jsonDropUrls);
        } catch (JSONException e) {
            // Shouldn't be possible to trigger this exception
            throw new RuntimeException("Cannot build JSONObject", e);
        }
        return jsonObject;
    }

    /**
     * Exports the {@link Contact} information as a JSON string from an {@link Identity}
     *
     * @param identity {@link Identity} to export {@link Contact} information from
     * @return {@link Contact} information as JSON string
     */
    public static String exportIdentityAsContact(Identity identity) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonDropUrls = new JSONArray();
        try {
            jsonObject.put(KEY_ALIAS, identity.getAlias());
            jsonObject.put(KEY_EMAIL, identity.getEmail());
            jsonObject.put(KEY_PHONE, identity.getPhone());
            jsonObject.put(KEY_PUBLIC_KEY, identity.getKeyIdentifier());
            for (DropURL dropURL : identity.getDropUrls()) {
                jsonDropUrls.put(dropURL);
            }
            jsonObject.put(KEY_DROP_URLS, jsonDropUrls);
        } catch (JSONException e) {
            // Shouldn't be possible to trigger this exception
            throw new RuntimeException("Cannot build JSONObject", e);
        }

        return jsonObject.toString();
    }

    /**
     * Parse a {@link Contact} from a {@link Contact} JSON string
     *
     * @param identity {@link Identity} for setting the owner of the {@link Contact}
     * @param json     {@link Contact} JSON string
     * @return {@link Contact} parsed from JSON string
     */
    public static Contact parseContactForIdentity(Identity identity, JSONObject json) throws JSONException {
        return parseContactFromJSON(identity, json);
    }


    /**
     * Parse {@link Contacts} from a {@link Contacts} JSON string
     *
     * @param identity   {@link Identity} for setting the owner of the {@link Contact}s
     * @param jsonObject {@link Contacts} JSONObject
     * @return {@link Contacts} parsed from JSON string
     */
    public static Contacts parseContactsForIdentity(Identity identity, JSONObject jsonObject) throws JSONException {
        Contacts contacts = new Contacts(identity);
        JSONArray jsonContacts = jsonObject.getJSONArray(KEY_CONTACTS);

        for (int i = 0; i < jsonContacts.length(); i++) {
            try {
                contacts.put(parseContactFromJSON(identity, jsonContacts.getJSONObject(i)));
            } catch (JSONException e) {
                Log.e(TAG, "Could not parese this contact. Will skip: " + e);
            }
        }
        if (contacts.getContacts().isEmpty()) {
            throw new JSONException("Could not find a valid contact entry in " + KEY_CONTACTS);
        }
        return contacts;
    }

    /**
     * Parses a JSON-String which can either be a contacts list or a single contact.
     * No matter what result will be wrapped in @see Contacts
     */

    public static Contacts parse(Identity identity, String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.has(KEY_CONTACTS)) {
            return parseContactsForIdentity(identity, jsonObject);
        } else {
            Contact singleContact = parseContactForIdentity(identity, jsonObject);
            Contacts wrapper = new Contacts(identity);
            wrapper.put(singleContact);
            return wrapper;
        }

    }


    @NonNull
    private static Contact parseContactFromJSON(Identity identity, JSONObject jsonObject) throws JSONException {
        Collection<DropURL> dropURLs = new ArrayList<>();
        String alias = jsonObject.getString(KEY_ALIAS);
        JSONArray jsonDropURLS = jsonObject.getJSONArray(KEY_DROP_URLS);
        for (int i = 0; i < jsonDropURLS.length(); i++) {
            try {
                dropURLs.add(new DropURL(jsonDropURLS.getString(i)));
            } catch (URISyntaxException e) {
                Log.w(TAG, "Could not parse uri: " + jsonDropURLS.getString(i) + " will ignore it", e);
            } catch (QblDropInvalidURL e) {
                Log.w(TAG, "Could not parse uri: " + jsonDropURLS.getString(i) + " will ignore it", e);
            }
        }
        String keyIdentifier = jsonObject.getString(KEY_PUBLIC_KEY);

        Contact contact = new Contact(alias, dropURLs, new QblECPublicKey(Hex.decode(keyIdentifier)));
        if (jsonObject.has(KEY_EMAIL)) {
            contact.setEmail(jsonObject.getString(KEY_EMAIL));
        }
        if (jsonObject.has(KEY_PHONE)) {
            contact.setPhone(jsonObject.getString(KEY_PHONE));
        }
        return contact;
    }
}
