package de.qabel.qabelbox.contacts.view.adapters

import android.view.View
import android.widget.LinearLayout
import com.nhaarman.mockito_kotlin.*
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.extensions.displayName
import de.qabel.qabelbox.contacts.view.widgets.ContactIconDrawable
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import de.qabel.qabelbox.ui.views.SquareFrameLayout
import de.qabel.qabelbox.ui.views.TextViewFont
import de.qabel.qabelbox.util.IdentityHelper
import kotlinx.android.synthetic.main.fragment_contact_details.view.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ContactDetailsAdapterTest {

    val identity = IdentityHelper.createIdentity("Identity B", "prefix");
    val contact = IdentityHelper.createContact("Kontakt A");

    @Test
    fun testLoadContact() {
        println(contact)
        println(contact.nickName)
        testView(ContactDto(contact, listOf(identity)))
    }

    @Test
    fun testLoadContactWithoutIdentities() {
        testView(ContactDto(contact, emptyList()))
    }

    @Test
    fun testLoadContactWithMultipleIdentities() {
        val identityC = IdentityHelper.createIdentity("Identity C", "prefixV");
        testView(ContactDto(contact, listOf(identity, identityC)))
    }

    fun testView(contactDto: ContactDto) {
        val ctx = RuntimeEnvironment.application;

        val initialsField: TextViewFont = mock()
        val nameField: TextViewFont = mock()
        val nickField: TextViewFont = mock()
        val dropField: TextViewFont = mock()
        val keyField: TextViewFont = mock()
        val actionContainer: LinearLayout = mock()
        val imageView: SquareFrameLayout = mock()

        val adapter = ContactDetailsAdapter({ identity -> Unit })
        adapter.view = mock()
        stub(adapter.view!!.context).toReturn(ctx)
        stub(adapter.view!!.tv_initial).toReturn(initialsField)
        stub(adapter.view!!.contact_icon_border).toReturn(imageView)
        stub(adapter.view!!.editTextContactName).toReturn(nameField)
        stub(adapter.view!!.editTextContactNick).toReturn(nickField)
        stub(adapter.view!!.editTextContactDropURL).toReturn(dropField)
        stub(adapter.view!!.editTextContactPublicKey).toReturn(keyField)
        stub(adapter.view!!.contact_details_actions).toReturn(actionContainer)

        adapter.loadContact(contactDto)

        verify(initialsField).text = any()
        if(contactDto.contact.displayName() == contactDto.contact.alias){
            verify(nameField).visibility = View.GONE
        }else {
            verify(nameField).text = contactDto.contact.alias
        }
        verify(nickField).text = contactDto.contact.nickName
        verify(dropField).text = any()
        verify(imageView).background = ContactIconDrawable(emptyList())
        verify(keyField).text = any()
        verify(actionContainer).removeAllViews()
        verify(actionContainer, times(contactDto.identities.size)).addView(any())
    }

}
