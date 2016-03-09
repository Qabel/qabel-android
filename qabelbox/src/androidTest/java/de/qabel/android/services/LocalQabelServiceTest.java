package de.qabel.android.services;

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
import de.qabel.android.exceptions.QblStorageEntityExistsException;

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

	public void testRetrieveIdentity() {
		Identities identities = mService.getIdentities();
		assertTrue(identities.getIdentities().contains(identity));
	}

	public void testGetActiveIdentity() {
		assertEquals(identity, mService.getActiveIdentity());
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
		mService.addContact(contact);
		assertTrue(mService.getContacts(identity).getContacts().contains(contact));
	}

	public void testAddDuplicateContact() throws QblStorageEntityExistsException {
		try {
			mService.addContact(contact);
			mService.addContact(contact);
		}catch (QblStorageEntityExistsException e){
			return;
		}
		fail("Expected QblStorageEntityExistsException");
	}

	public void testDeleteContact() throws QblStorageEntityExistsException {
		mService.addContact(contact);
		assertTrue(mService.getContacts(identity).getContacts().contains(contact));
		mService.deleteContact(contact);
		assertFalse(mService.getContacts(identity).getContacts().contains(contact));
	}

	public void testModifyContact() throws QblStorageEntityExistsException {
		mService.addContact(contact);
		contact.setAlias("bar");
		mService.modifyContact(contact);
		assertTrue(mService.getContacts(identity).getContacts().contains(contact));
		contact.setAlias("foo");
		assertFalse(mService.getContacts(identity).getContacts().contains(contact));
	}

	public void testGetAllContacts() throws QblStorageEntityExistsException {
		mService.addContact(contact);
		Identity secondIdentity = new Identity("bar", null, new QblECKeyPair());
		mService.addIdentity(identity);
		Contact secondContact = new Contact("blub", null, new QblECKeyPair().getPub());
		mService.addContact(secondContact, secondIdentity);
		Map<Identity, Contacts> contacts = mService.getAllContacts();
		assertEquals(2, contacts.size());
		assertTrue(contacts.containsKey(identity));
		assertTrue(contacts.containsKey(secondIdentity));
		assertTrue(contacts.get(identity).getContacts().contains(contact));
		assertTrue(contacts.get(secondIdentity).getContacts().contains(secondContact));
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
		mService.addContact(recipientContact);
		mService.addContact(senderContact);

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
