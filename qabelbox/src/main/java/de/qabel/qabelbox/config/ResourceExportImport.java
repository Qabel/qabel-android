package de.qabel.qabelbox.config;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;

public class ResourceExportImport {

    private static final String TAG_QABEL_ALIAS = "QABELALIAS";
    private static final String TAG_QABEL_DROP_URLS = "QABELDROPURL";
    private static final String TAG_QABEL_KEY_IDENTIFIER = "QABELKEYIDENTIFIER";

    /**
     * Exports the {@link Contact} information as a JSON string from an {@link Identity}
     * @param identity {@link Identity} to export {@link Contact} information from
     * @return {@link Contact} information as JSON string
     */
    public static String exportIdentityAsContact(Identity identity) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonDropUrls = new JSONArray();
        try {
            jsonObject.put(TAG_QABEL_ALIAS, identity.getAlias());
            for (DropURL dropURL : identity.getDropUrls()) {
                jsonDropUrls.put(dropURL);
            }
            jsonObject.put(TAG_QABEL_DROP_URLS, jsonDropUrls);
            jsonObject.put(TAG_QABEL_KEY_IDENTIFIER, identity.getKeyIdentifier());
        } catch (JSONException e) {
            // Shouldn't be possible to trigger this exception
            throw new RuntimeException("Cannot build JSONObject", e);
        }

        return jsonObject.toString();
    }

    /**
     * Parse a {@link Contact} from a {@link Contact} JSON string
     * @param identity {@link Identity} for setting the owner of the {@link Contact}
     * @param json {@link Contact} JSON string
     * @return {@link Contact} parsed from JSON string
     * @throws JSONException
     * @throws URISyntaxException
     * @throws QblDropInvalidURL
     */
    public static Contact parseContactForIdentity(Identity identity, String json) throws JSONException, URISyntaxException, QblDropInvalidURL {
        JSONObject jsonObject = new JSONObject(json);

        Collection<DropURL> dropURLs = new ArrayList<>();
        String alias = jsonObject.getString(TAG_QABEL_ALIAS);
        JSONArray jsonDropURLS = jsonObject.getJSONArray(TAG_QABEL_DROP_URLS);
        for (int i = 0; i < jsonDropURLS.length(); i++) {
            dropURLs.add(new DropURL(jsonDropURLS.getString(i)));
        }
        String keyIdentifier = jsonObject.getString(TAG_QABEL_KEY_IDENTIFIER);

        return new Contact(identity, alias, dropURLs, new QblECPublicKey(Hex.decode(keyIdentifier)));
    }
}
