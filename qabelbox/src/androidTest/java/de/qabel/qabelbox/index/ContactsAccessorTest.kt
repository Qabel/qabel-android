package de.qabel.qabelbox.index

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsAccessorTest {

    lateinit var accessor: ExternalContactsAccessor

    @Before
    fun setUp() {
        accessor = AndroidContactsAccessor(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testGetContacts() {
        accessor.getContacts().sortedBy { it.displayName }.let {
            println("SHOW ${it.size} Contacts")
            it.forEach {
                println(it.displayName + "\t" + it.mobilePhoneNumbers.joinToString() + "\t" + it.emailAddresses.joinToString())
            }
        }
        Thread.sleep(2000)
    }

}
