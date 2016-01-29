package de.qabel.qabelbox.config;

import org.json.JSONException;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ContactExportImportTest {

    @Test
    public void testExportIdentityAsContact() throws URISyntaxException, QblDropInvalidURL {
        QblECKeyPair qblECKeyPair = new QblECKeyPair();
        Collection<DropURL> dropURLs = new ArrayList<>();
        dropURLs.add(
                new DropURL(
                        "http://localhost:6000/1234567890123456789012345678901234567891234"));
        Identity identity = new Identity("Identity", dropURLs, qblECKeyPair);

        assertThat(ContactExportImport.exportIdentityAsContact(identity), is("{\"QABELALIAS\":\"Identity\",\"QABELDROPURL\":" +
                        "[\"http:\\/\\/localhost:6000\\/1234567890123456789012345678901234567891234\"]," +
                        "\"QABELKEYIDENTIFIER\":\"" + qblECKeyPair.getPub().getReadableKeyIdentifier() + "\"}"));
    }

    @Test
    public void testImportExportedContact() throws URISyntaxException, QblDropInvalidURL, JSONException {
        QblECKeyPair qblECKeyPair = new QblECKeyPair();
        Collection<DropURL> dropURLs = new ArrayList<>();
        dropURLs.add(
                new DropURL(
                        "http://localhost:6000/1234567890123456789012345678901234567891234"));
        dropURLs.add(
                new DropURL(
                        "http://localhost:6000/0000000000000000000000000000000000000000000"));

        Identity identity = new Identity("Identity", dropURLs, qblECKeyPair);

        String json = ContactExportImport.exportIdentityAsContact(identity);
        // Normally a contact wouldn't be imported for the belonging identity, but it doesn't matter for the test
        Contact contact = ContactExportImport.parseContactForIdentity(identity, json);

        assertThat(identity.getAlias(), is(contact.getAlias()));
        assertThat(identity.getDropUrls(), is(contact.getDropUrls()));
        assertThat(identity.getEcPublicKey().getReadableKeyIdentifier(), is(contact.getEcPublicKey().getReadableKeyIdentifier()));
    }
}
