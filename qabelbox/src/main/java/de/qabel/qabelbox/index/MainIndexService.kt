package de.qabel.qabelbox.index

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.index.*
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import java.util.*

//TODO coreable
class MainIndexService(private val indexServer: IndexServer,
                       private val contactRepository: ContactRepository,
                       private val identityRepository: IdentityRepository) : IndexService {

    override fun updateIdentity(identity: Identity) {
        val uploadAction = UpdateIdentity.fromIdentity(identity, UpdateAction.CREATE)
        indexServer.updateIdentity(uploadAction)
    }

    override fun updateIdentityPhone(updatedIdentity: Identity, oldPhone: String) {
        createFieldUpdate(updatedIdentity, FieldType.PHONE, updatedIdentity.phone, oldPhone).apply {
            indexServer.updateIdentity(this)
        }
    }

    override fun updateIdentityEmail(updatedIdentity: Identity, oldEmail: String) {
        createFieldUpdate(updatedIdentity, FieldType.EMAIL, updatedIdentity.email, oldEmail).apply {
            indexServer.updateIdentity(this)
        }
    }

    private fun createFieldUpdate(updatedIdentity: Identity, fieldType: FieldType, newValue: String, oldValue: String): UpdateIdentity =
            UpdateIdentity(
                    keyPair = updatedIdentity.primaryKeyPair,
                    dropURL = updatedIdentity.helloDropUrl,
                    alias = updatedIdentity.alias,
                    fields = mutableListOf<UpdateField>().apply {
                        if (!oldValue.isEmpty()) {
                            add(UpdateField(UpdateAction.DELETE, fieldType, oldValue))
                        }
                        if (!newValue.isEmpty()) {
                            UpdateField(UpdateAction.CREATE, fieldType, newValue)
                        }
                    })

    override fun updateIdentityVerifications() {
        val identities = identityRepository.findAll()
        identities.identities.forEach { identity ->
            val phoneNumberVerified: Boolean =
                    indexServer.searchForPhone(identity.phone).any {
                        it.publicKey.equals(identity.ecPublicKey)
                    }
            val emailVerified: Boolean =
                    indexServer.searchForMail(identity.email).any {
                        it.publicKey.equals(identity.ecPublicKey)
                    }
            //TODO Update new fields
        }
    }

    override fun deleteIdentity(identity: Identity) {
        UpdateIdentity.fromIdentity(identity, UpdateAction.DELETE).let {
            indexServer.updateIdentity(it)
        }
    }

    /**
     * Search the index for the external contacts and updates the local contact repository.
     * Returns a list of new contacts.
     *
     * TODO With my last android researches my phone would require ca. 200(contacts) * (mail per contact + phone per contact) requests
     * We need an interface to search for multiple values with "OR"
     *
     */
    override fun syncContacts(externalContacts: List<RawContact>): List<Contact> {
        //Map lists of values to a list of fieldType and value associated with its raw contact
        val searchValues: Map<RawContact, List<Pair<FieldType, String>>> =
                externalContacts.associate {
                    Pair(it, mutableListOf<Pair<FieldType, String>>().apply {
                        addAll(it.emailAddresses.map {
                            Pair(FieldType.EMAIL, it)
                        })
                        addAll(it.mobilePhoneNumbers.map {
                            Pair(FieldType.PHONE, it)
                        })
                    })
                }

        val indexResults: Map<RawContact, List<IndexContact>> =
                searchValues.keys.associate {
                    //Use hashSet to filter duplicate results
                    val results = HashSet<IndexContact>()
                    searchValues[it]?.forEach {
                        indexServer.search(mapOf(it)).forEach {
                            results.add(it)
                        }
                    }
                    Pair(it, results.toList())
                }

        val newContacts = mutableListOf<Contact>()
        val identities = identityRepository.findAll()
        val singleIdentityMode = identities.identities.size == 1
        indexResults.map {
            val rawContact = it.key
            it.value.forEach {
                val receivedContact = it.toContact()
                if (contactRepository.exists(receivedContact)) {
                    val localContact = contactRepository.findByKeyId(receivedContact.keyIdentifier)
                    //Override nick with existing name
                    if (localContact.alias == localContact.nickName) {
                        localContact.nickName = rawContact.displayName
                    }

                    //TODO May sort by prio, in android there should be a main number or something
                    if (localContact.phone.isEmpty() && !rawContact.mobilePhoneNumbers.isEmpty()) {
                        localContact.phone = rawContact.mobilePhoneNumbers.first()
                    }
                    if (localContact.email.isEmpty() && !rawContact.emailAddresses.isEmpty()) {
                        localContact.email = rawContact.emailAddresses.first()
                    }
                } else {
                    val firstIdentity = identities.entities.first()
                    if (singleIdentityMode) {
                        contactRepository.save(receivedContact, firstIdentity)
                    } else {
                        //TODO add persist method like new update method
                        contactRepository.save(receivedContact, firstIdentity)
                        //Dont associate
                        contactRepository.update(receivedContact, emptyList())
                    }
                    newContacts.add(receivedContact)
                }
            }
        }
        return newContacts
    }
}
