package de.qabel.qabelbox.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.EntitySelection
import de.qabel.qabelbox.contacts.extensions.colorForKeyIdentitfier
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import kotlinx.android.synthetic.main.item_identity_for_chooser.view.*
import org.jetbrains.anko.layoutInflater

/**
 * ArrayAdapter for Identities
 */
class EntitySelectionAdapter(val ctx: Context, val identities: List<EntitySelection>): ArrayAdapter<EntitySelection>(ctx,
        R.layout.item_identity_for_chooser, R.id.item_name , identities) {

    val iconSize = ctx.resources.
            getDimension(R.dimen.material_drawer_item_profile_icon_width).toInt()

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return getView(position, convertView, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: ctx.layoutInflater.inflate(R.layout.item_identity_for_chooser, parent, false)
        val item = getItem(position)
        view.item_name.text = item.alias
        view.item_icon.background = IdentityIconDrawable(
                width = iconSize,
                height = iconSize,
                text = item.initials(),
                color = colorForKeyIdentitfier(item.keyId, 0, ctx))
        return view
    }
}

