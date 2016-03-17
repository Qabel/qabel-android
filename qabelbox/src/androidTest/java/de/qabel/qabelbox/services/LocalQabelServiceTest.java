package de.qabel.qabelbox.services;

import android.content.Intent;
import android.test.ServiceTestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.repository.exception.EntityNotFoundExcepion;
import de.qabel.qabelbox.repository.exception.PersistenceException;

public class LocalQabelServiceTest extends ServiceTestCase<LocalQabelServiceTester> {

	private LocalQabelServiceTester mService;
	private Identity identity;
	private Contact contact;

	public LocalQabelServiceTest() {
		super(LocalQabelServiceTester.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		getContext().deleteDatabase(LocalQabelServiceTester.DB_NAME);
		Intent intent = new Intent(getContext(), LocalQabelServiceTester.class);
		startService(intent);
		this.mService = getService();
		identity = new Identity("foo", null, new QblECKeyPair());
		mService.addIdentity(identity);
		mService.setActiveIdentity(identity);

		contact = new Contact("foo", null, new QblECKeyPair().getPub());
	}

	public void testAddIdentity() {
		Identity newIdentity = new Identity("zbrazo", null, new QblECKeyPair());
		mService.addIdentity(newIdentity);
		try {
			Identity retrivedFromRepo = mService.identityRepository.find(newIdentity.getKeyIdentifier());
			assertNotNull(retrivedFromRepo);
			assertIdentityPublicContentEquals(newIdentity, retrivedFromRepo);
			Identity retrivedFromService = mService.getIdentities().getByKeyIdentifier(newIdentity.getKeyIdentifier());
			assertNotNull(retrivedFromService);
			assertIdentityPublicContentEquals(newIdentity, retrivedFromService);

		} catch (EntityNotFoundExcepion entityNotFoundExcepion) {
			entityNotFoundExcepion.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		}

	}

	private void assertIdentityPublicContentEquals(Identity lhsIdentity, Identity rhsIdentity) {
		assertEquals(lhsIdentity.getAlias(), rhsIdentity.getAlias());
		assertEquals(lhsIdentity.getEmail(), rhsIdentity.getEmail());
		assertEquals(lhsIdentity.getKeyIdentifier(), rhsIdentity.getKeyIdentifier());
		assertEquals(lhsIdentity.getEcPublicKey(), rhsIdentity.getEcPublicKey());
		assertEquals(lhsIdentity.getPrefixes(), rhsIdentity.getPrefixes());


	}


	public void testRetrieveIdentity() {
		Identities identities = mService.getIdentities();
		assertTrue(identities.getIdentities().contains(identity));
	}

	public void testGetActiveIdentity() {
		assertTrue(mService.getIdentities().getIdentities().size() > 0);
		Identity retrievedActiveIdentity = mService.getActiveIdentity();
		assertTrue(mService.getIdentities().getIdentities().size() > 0);
		assertNotNull(retrievedActiveIdentity);
		assertEquals(identity, retrievedActiveIdentity);
	}

	public void testDeleteIdentity() {
		mService.deleteIdentity(identity);
		Identities identities = mService.getIdentities();
		assertFalse(identities.getIdentities().contains(identity));
		assertNull(mService.getActiveIdentity());
	}

	public void testModifyIdentity() {
		identity.setAlias("bar");
		mService.modifyIdentity(identity);
		assertEquals(identity.getAlias(), mService.getActiveIdentity().getAlias());
	}

	public void testAddContact() throws QblStorageEntityExistsException {
		try {
			mService.addContact(contact);
			assertTrue(mService.getContacts(identity).getContacts().contains(contact));
		} catch (PersistenceException e) {
			fail("Unexpected PersistenceException " + e);
		}
	}

	public void testAddDuplicateContact() throws QblStorageEntityExistsException {
		try {
			mService.addContact(contact);
			mService.addContact(contact);
		}catch (QblStorageEntityExistsException e){
			return;
		} catch (PersistenceException e) {
			fail("Unexpected PersistenceException " + e);
		}
		fail("Expected QblStorageEntityExistsException");
	}

	public void testDeleteContact() throws QblStorageEntityExistsException {
		try {
			mService.addContact(contact);
			assertTrue(mService.getContacts(identity).getContacts().contains(contact));
			mService.deleteContact(contact);
			assertFalse(mService.getContacts(identity).getContacts().contains(contact));
		} catch (PersistenceException e) {
			fail("Unexpected PersistenceException " + e);
		}
	}

	public void testModifyContact() throws QblStorageEntityExistsException {
		try {
			mService.addContact(contact);
			assertTrue(mService.getContacts(identity).getContacts().contains(contact));
			contact.setAlias("bar");
			mService.modifyContact(contact);
			assertTrue(mService.getContacts(identity).getContacts().contains(contact));

			contact.setAlias("foo");
			// TODO: Next line is not true for any cached implementation, because we hold the same reference as the cache
			// assertFalse(mService.getContacts(identity).getContacts().contains(contact));
			contact = new Contact("foo", null, new QblECKeyPair().getPub());
			assertFalse(mService.getContacts(identity).getContacts().contains(contact));
		} catch (PersistenceException e) {
			fail("Unexpected PersistenceException " + e);
		}
	}

	public void testGetAllContacts() throws QblStorageEntityExistsException {
		try {
			mService.addContact(contact);
			Identity secondIdentity = new Identity("bar", null, new QblECKeyPair());
			mService.addIdentity(secondIdentity);

			Contact secondContact = new Contact("blub", null, new QblECKeyPair().getPub());
			mService.addContact(secondContact, secondIdentity);

			Map<Identity, Contacts> contacts = null;
			try {
				contacts = mService.getAllContacts();
			} catch (EntityNotFoundExcepion entityNotFoundExcepion) {
				fail("Contacts not found");
			}
			assertEquals(2, contacts.size());
			assertTrue(contacts.containsKey(identity));
			assertTrue(contacts.containsKey(secondIdentity));
			assertTrue(contacts.get(identity).getContacts().contains(contact));
			Contacts secondIdentitiesContacts = contacts.get(secondIdentity);
			assertTrue(secondIdentitiesContacts.getContacts().contains(secondContact));
		} catch (PersistenceException e) {
			fail("Unexpected PersistenceException " + e);
		}
	}

	public void testSendAndReceiveDropMessage() throws QblDropPayloadSizeException, URISyntaxException, QblDropInvalidURL, InterruptedException, QblStorageEntityExistsException {
		QblECKeyPair senderKeypair = new QblECKeyPair();
		Identity senderIdentity = new Identity("SenderIdentity", new ArrayList<DropURL>(), senderKeypair);

		QblECKeyPair receiverKeypair = new QblECKeyPair();
		Identity receiverIdentity = new Identity("ReceiverIdentity", new ArrayList<DropURL>(), receiverKeypair);

		Contact senderContact = new Contact("foo", null, senderKeypair.getPub());

		Contact recipientContact = new Contact("foo", null, receiverKeypair.getPub());
		recipientContact.addDrop(new DropURL("http://localhost/abcdefghijklmnopqrstuvwxyzabcdefgworkingUrl"));

		mService.addIdentity(senderIdentity);
		mService.addIdentity(receiverIdentity);
		try {
			mService.addContact(recipientContact);
			mService.addContact(senderContact);
		} catch (PersistenceException e) {
			fail("Unexpected PersistenceException: " + e);
		}
		DropMessage dropMessage = new DropMessage(senderIdentity, "DropPayload", "DropPayloadType");

		final CountDownLatch lock = new CountDownLatch(1);

		mService.sendDropMessage(dropMessage, recipientContact, senderIdentity,
				new LocalQabelService.OnSendDropMessageResult() {
					@Override
					public void onSendDropResult(Map<DropURL, Boolean> deliveryStatus) {
						lock.countDown();
					}
				});

		lock.await();

		Collection<DropMessage> dropMessages =
				mService.retrieveDropMessages(URI.create("http://localhost/dropmessages"), 0);

		assertEquals(1, dropMessages.size());
	}

	public void testReceiveDropMessagesEmpty() {
		Collection<DropMessage> dropMessages =
				mService.retrieveDropMessages(URI.create("http://localhost/empty"), 0);

		assertEquals(0, dropMessages.size());
	}

}
