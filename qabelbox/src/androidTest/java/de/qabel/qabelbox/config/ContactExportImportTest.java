package de.qabel.qabelbox.config;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;
import de.qabel.qabelbox.repository.exception.PersistenceException;
import de.qabel.qabelbox.services.LocalQabelService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ContactExportImportTest {

	private static final String DROP_URL_1 = "http://localhost:6000/1234567890123456789012345678901234567891234";
	private static final String DROP_URL_2 = "http://localhost:6000/0000000000000000000000000000000000000000000";

	public static final String MAVERICK_ALIAS="Christopher Blair";
	public static final String MAVERICK_PUBLICKEYID="b3a4208bd545701f3bcd5eee508cd0f83e4a56f1518c6159ecd96f2ff86de172";
	public static final String MAVERICK_DROP="https://test-drop.qabel.de/AIXqM7n_hjTfpgPrvsDeWX6dc2Yn4F7OfyCtlX52Zkk";

	public static final String MANIAC_ALIAS="Todd Marshall";
	public static final String MANIAC_PUBLICKEYID="be14d35443af65a750c941fbd20ea16d678a03ac0f3c3bf42448776252a89961";
	public static final String MANIAC_DROP="https://test-drop.qabel.de/E9yoIvTEAyYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

	public static final String AT_ALIAS="Admiral Tolwyn";
	public static final String AT_PUBLICKEYID="5a4b033b326840438e52f4fbe1d995da0b7276387d18862b9242767ca9cbb752";
	public static final String AT_DROP="https://test-drop.qabel.de/Cg7hjB0zlQ3bubsGfRAAAAAAAAAAAAAAAAAAAAAAAAA";

	public static final String ANGEL_ALIAS="Jeannette Devereaux";
	public static final String ANGEL_PUBLICKEYID="e9468068367fbf3a98784bf3ec014aab7a9f4254a17af185f2811f6263c0405e";
	public static final String ANGEL_DROP="https://test-drop.qabel.de/Cg7hjB0zlQ3bubsGfRrVggAAAAAAAAAAAAAAAAAAAAA";


	public static final String JSON_SINGLE_CONTACT="{\n" +
			"\t\"public_key\": \"7c879f241a891938d0be68fbc178ced6f926c95385f588fe8924d0d81a96a32a\",\n" +
			"\t\"drop_urls\": [\"https://qdrop.prae.me/APlvHMq05d8ylgp64DW2AHFmdJj2hYDQXJiSnr-Holc\"],\n" +
			"\t\"alias\": \"Zwei\"\n" +
			"}";

	public static final String JSON_CONTACTLIST_WITH_INVALID_ENTRY="{\n" +
			"\t\"contacts\": [{\n" +
			"\t\t\"public_key\": \"7c879f241a891938d0be68fbc178ced6f926c95385f588fe8924d0d81a96a32a\",\n" +
			"\t\t\"drop_urls\": [\"https://qdrop.prae.me/APlvHMq05d8ylgp64DW2AHFmdJj2hYDQXJiSnr-Holc\"]\n" +
			"\t}, {\n" +
			"\t\t\"public_key\": \"333b48c161f60bed0b116883aadac3c9217feff8225276561959d9b826944b69\",\n" +
			"\t\t\"drop_urls\": [\"https://qdrop.prae.me/AI2X-QJ8WI2VtMgT0y302RyY3wU_RoAsqdaFQ9gf9fs\"],\n" +
			"\t\t\"alias\": \"Eins\"\n" +
			"\t}]\n" +
			"}";

	public static final String JSON_CONTACTLIST_INVALID_ENTRIES = "{\n" +
			"\t\"contacts\": [{\n" +
			"\t\t\"public_key\": \"7c879f241a891938d0be68fbc178ced6f926c95385f588fe8924d0d81a96a32a\",\n" +
			"\t\t\"drop_urls\": [\"https://qdrop.prae.me/APlvHMq05d8ylgp64DW2AHFmdJj2hYDQXJiSnr-Holc\"]\n" +
			"\t}, {\n" +
			"\t\t\"public_key\": \"333b48c161f60bed0b116883aadac3c9217feff8225276561959d9b826944b69\",\n" +
			"\t\t\"drop_urls\": [\"https://qdrop.prae.me/AI2X-QJ8WI2VtMgT0y302RyY3wU_RoAsqdaFQ9gf9fs\"]\n" +
			"\t}]\n" +
			"}";

	public static final String JSON_CONTACTLIST_TIGERSCLAW="{\n" +
			"\t\"contacts\": [{\n" +
			"\t\t\"public_key\": \""+ MAVERICK_PUBLICKEYID+"\",\n" +
			"\t\t\"drop_urls\": [\""+ MAVERICK_DROP+"\"],\n" +
			"\t\t\"alias\": \"" + MAVERICK_ALIAS+"\"\n" +
			"\t}, {\n" +
			"\t\t\"public_key\": \""+MANIAC_PUBLICKEYID+"\",\n" +
			"\t\t\"drop_urls\": [\""+MANIAC_DROP+"\"],\n" +
			"\t\t\"alias\": \""+MANIAC_ALIAS+"\"\n" +
			"\t}, {\n" +
			"\t\t\"public_key\": \""+AT_PUBLICKEYID+"\",\n" +
			"\t\t\"drop_urls\": [\""+AT_DROP+"\"],\n" +
			"\t\t\"alias\": \""+AT_ALIAS+"\"\n" +
			"\t}, {\n" +
			"\t\t\"public_key\": \""+ANGEL_PUBLICKEYID+"\",\n" +
			"\t\t\"drop_urls\": [\""+ANGEL_DROP+"\"],\n" +
			"\t\t\"alias\": \""+ANGEL_ALIAS+"\"\n" +
			"\t}]\n" +
			"}";

	Identity identity;
	Contact contact1;
	Contact contact2;
	Contact mavericksContact,maniacsContact,atsContact,angelsContact;
	Contacts contacts;
	QblECKeyPair qblECKeyPair;

	@Before
	public void setUp() throws Exception {
		qblECKeyPair = new QblECKeyPair();
		Collection<DropURL> dropURLs = new ArrayList<>();
		dropURLs.add(new DropURL(DROP_URL_1));
		dropURLs.add(new DropURL(DROP_URL_2));
		identity = new Identity("Identity", dropURLs, qblECKeyPair);
		identity.setEmail("test@example.com");
		identity.setPhone("+491111111");



		QblECKeyPair contact1KeyPair = new QblECKeyPair();
		contact1 = new Contact("Contact1", dropURLs, contact1KeyPair.getPub());

		QblECKeyPair contact2KeyPair = new QblECKeyPair();
		contact2 = new Contact("Contact2", dropURLs, contact2KeyPair.getPub());

		contacts = new Contacts(identity);
		contacts.put(contact1);
		contacts.put(contact2);
		Iterator<Contact> contacts = initTestContacts().iterator();
		mavericksContact = contacts.next();
		maniacsContact = contacts.next();
		atsContact = contacts.next();
		angelsContact = contacts.next();
	}

	public static List<Contact> initTestContacts() throws URISyntaxException, QblDropInvalidURL {
		List<Contact> result = new LinkedList<>();
		result.add(initContact(MAVERICK_ALIAS, MAVERICK_PUBLICKEYID, MAVERICK_DROP));
		result.add(initContact(MANIAC_ALIAS, MANIAC_PUBLICKEYID, MANIAC_DROP));
		result.add(initContact(AT_ALIAS, AT_PUBLICKEYID, AT_DROP));
		result.add(initContact(ANGEL_ALIAS, ANGEL_PUBLICKEYID, ANGEL_DROP));
		return result;
	}

	public static Contact initContact(String alias, String publicKey, String... initialDropURLs) throws URISyntaxException, QblDropInvalidURL {
		Collection<DropURL> dropURLs = new ArrayList<>();
		for (String url : initialDropURLs) {
			dropURLs.add(new DropURL(url));
		}
		QblECPublicKey pubKey=new QblECPublicKey(Hex.decode(publicKey));
		return new Contact(alias,dropURLs,pubKey);
	}

	@Test
	public void testExportImportContact() throws QblDropInvalidURL, JSONException, URISyntaxException {
		String contactJSON = ContactExportImport.exportContact(contact1);
		Contact importedContact1 = ContactExportImport.parseContactForIdentity(identity, new JSONObject(contactJSON));
		assertContactEquals(contact1, importedContact1);
	}

	@Test
	public void testExportImportContactWithOptionals() throws QblDropInvalidURL, JSONException, URISyntaxException {
		contact1.setEmail("test@example.com");
		contact1.setPhone("+491111111");
		String contactJSON = ContactExportImport.exportContact(contact1);
		Contact importedContact1 = ContactExportImport.parseContactForIdentity(identity, new JSONObject(contactJSON));
		assertContactEquals(contact1, importedContact1);
	}

	@Test
	public void testExportImportContacts() throws JSONException, URISyntaxException, QblDropInvalidURL {
		String contactsJSON = ContactExportImport.exportContacts(contacts);

		Contacts importedContacts = ContactExportImport.parseContactsForIdentity(identity, new JSONObject(contactsJSON));

		assertThat(importedContacts.getContacts().size(), is(2));
		Contact importedContact1 = importedContacts.getByKeyIdentifier(contact1.getKeyIdentifier());
		Contact importedContact2 = importedContacts.getByKeyIdentifier(contact2.getKeyIdentifier());

		assertContactEquals(contact1, importedContact1);
		assertContactEquals(contact2, importedContact2);
	}

	private void assertContactEquals(Contact contact1, Contact contact2){
		assertThat(contact1.getAlias(), is(contact2.getAlias()));
		assertThat(contact1.getDropUrls(), is(contact2.getDropUrls()));
		assertThat(contact1.getEcPublicKey().getReadableKeyIdentifier(), is(contact2.getEcPublicKey().getReadableKeyIdentifier()));
		assertThat(contact1.getPhone(), is(contact2.getPhone()));
		assertThat(contact1.getEmail(), is(contact2.getEmail()));
	}

    @Test
    public void testImportExportedContactFromIdentity() throws URISyntaxException, QblDropInvalidURL, JSONException {
         String json = ContactExportImport.exportIdentityAsContact(identity);
        // Normally a contact wouldn't be imported for the belonging identity, but it doesn't matter for the test
        Contact contact = ContactExportImport.parseContactForIdentity(identity, new JSONObject(json));

        assertThat(identity.getAlias(), is(contact.getAlias()));
        assertThat(identity.getDropUrls(), is(contact.getDropUrls()));
        assertThat(identity.getEcPublicKey().getReadableKeyIdentifier(), is(contact.getEcPublicKey().getReadableKeyIdentifier()));
		assertThat(identity.getPhone(), is(contact.getPhone()));
		assertThat(identity.getEmail(), is(contact.getEmail()));
    }


	@Test
	public void testImportPartialValidContactsList() {
		try {
			Contacts result = ContactExportImport.parseContactsForIdentity(identity, new JSONObject(JSON_CONTACTLIST_WITH_INVALID_ENTRY));
			assertEquals(1, result.getContacts().size());
			Contact first=result.getContacts().iterator().next();
			Contact expectedContact=initContact("Eins","333b48c161f60bed0b116883aadac3c9217feff8225276561959d9b826944b69","https://qdrop.prae.me/AI2X-QJ8WI2VtMgT0y302RyY3wU_RoAsqdaFQ9gf9fs");
			assertContactEquals(expectedContact, first);
		} catch (JSONException e) {
			fail("Could not parse JSON: "+e);
		} catch (URISyntaxException e) {
			fail("Could not parse JSON: " + e);
		} catch (QblDropInvalidURL qblDropInvalidURL) {
			fail("Could not parse JSON: " + qblDropInvalidURL);
		}


	}

	@Test
	public void testDetectLists() throws URISyntaxException, QblDropInvalidURL {
		try {
			Contacts tigersClawCrew=ContactExportImport.parse(identity,JSON_CONTACTLIST_TIGERSCLAW);
			assertEquals(4,tigersClawCrew.getContacts().size());

			Contacts singleContact = ContactExportImport.parse(identity, JSON_SINGLE_CONTACT);
			assertEquals(1,singleContact.getContacts().size());
			Contact expectedContact = initContact("Zwei", "7c879f241a891938d0be68fbc178ced6f926c95385f588fe8924d0d81a96a32a", "https://qdrop.prae.me/APlvHMq05d8ylgp64DW2AHFmdJj2hYDQXJiSnr-Holc");
			Contact myContact=singleContact.getContacts().iterator().next();
			assertContactEquals(expectedContact,myContact);

		} catch (JSONException e) {
			fail("Could not parse valid list: "+e);
		}

	}

	public static void deleteTestContacts() throws PersistenceException {
		LocalQabelService service = new LocalQabelService();
		try {
			for (Contact c : initTestContacts()) {
				service.deleteContact(c);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (QblDropInvalidURL qblDropInvalidURL) {
			qblDropInvalidURL.printStackTrace();
		}

	}


}
