package de.qabel.qabelbox.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.ui.displayName
import de.qabel.qabelbox.R
import de.qabel.qabelbox.helper.QRCodeHelper
import de.qabel.qabelbox.ui.extensions.setOrGone
import kotlinx.android.synthetic.main.fragment_barcode_shower.*

class QRCodeFragment : BaseFragment() {

    private lateinit var contact: Contact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contact = (arguments.getSerializable(ARG_CONTACT) ?: throw IllegalArgumentException("No contact given")) as Contact
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_barcode_shower, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editTextContactName.text = contact.displayName()
        contact_email.setOrGone(contact.email)
        contact_phone.setOrGone(contact.phone)
        QRCodeHelper().generateQRCode(activity, contact, qrcode)
    }

    override val title: String
        get() = getString(R.string.headline_qrcode)

    companion object {

        private val ARG_CONTACT = "Contact"

        fun newInstance(identity: Identity): QRCodeFragment {
            val fragment = QRCodeFragment()
            val args = Bundle()
            args.putSerializable(ARG_CONTACT, identity.toContact())
            fragment.arguments = args
            return fragment
        }

        fun newInstance(contact: Contact): QRCodeFragment {
            val fragment = QRCodeFragment()
            val args = Bundle()
            args.putSerializable(ARG_CONTACT, contact)
            fragment.arguments = args
            return fragment
        }
    }
}
