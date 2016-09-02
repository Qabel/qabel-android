package de.qabel.qabelbox.fragments

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.ui.extensions.setOrGone
import kotlinx.android.synthetic.main.fragment_create_identity_header.*

class CreateIdentityHeaderFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle): View? {
        return inflater.inflate(R.layout.fragment_create_identity_header, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(null)
    }

    fun updateUI(name: String?, email: String = "", phone: String = "") {
        if (name == null || name.isEmpty()) {
            logo_layout.visibility = View.VISIBLE
            initial_layout.visibility = View.INVISIBLE
        } else {
            logo_layout.visibility = View.INVISIBLE
            initial_layout.visibility = View.VISIBLE

            tv_email.setOrGone(email)
            tv_phone.setOrGone(phone)

            tv_name.text = name
            tv_initial.text = getInitials(name)

            tv_initial.forceLayout()
            initial_layout.forceLayout()
        }
    }

    private fun getInitials(name: String): String =
            name.split(" ".toRegex()).take(2).map {
                it.take(1).toUpperCase()
            }.joinToString("")
}
