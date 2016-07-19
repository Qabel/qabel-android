package de.qabel.qabelbox.contacts.view.adapters

import android.widget.LinearLayout
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.extensions.contactColors
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import de.qabel.qabelbox.ui.views.TextViewFont
import de.qabel.qabelbox.util.IdentityHelper
import kotlinx.android.synthetic.main.fragment_contact_details.view.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class,
        shadows = arrayOf(TextViewFontShadow::class), manifest = "src/main/AndroidManifest.xml")
class ContactDetailsAdapterTest {


    @Test
    fun testLoadContact(){
        val ctx = RuntimeEnvironment.application;
        val identity = IdentityHelper.createIdentity("Identity B", "prefix");
        val contact = IdentityHelper.createContact("Kontakt A");
        val contactDto = ContactDto(contact, listOf(identity));

        val initialsField: TextViewFont = mock()
        val nameField: TextViewFont = mock();
        val dropField: TextViewFont = mock()
        val keyField: TextViewFont = mock()
        val actionContainer : LinearLayout = mock()

        val adapter = ContactDetailsAdapter({identity -> Unit})
        adapter.view = mock()
        stub(adapter.view!!.context).toReturn(ctx)
        stub(adapter.view!!.tv_initial).toReturn(initialsField)
        stub(adapter.view!!.editTextContactName).toReturn(nameField)
        stub(adapter.view!!.editTextContactDropURL).toReturn(dropField)
        stub(adapter.view!!.editTextContactPublicKey).toReturn(keyField)
        stub(adapter.view!!.contact_details_actions).toReturn(actionContainer)

        adapter.loadContact(contactDto)

        verify(initialsField).text = any()
        verify(nameField).text = contact.alias
        verify(dropField).text = any()
        verify(keyField).text = any()
        verify(actionContainer).removeAllViews()
        verify(actionContainer).addView(any())
    }

}
