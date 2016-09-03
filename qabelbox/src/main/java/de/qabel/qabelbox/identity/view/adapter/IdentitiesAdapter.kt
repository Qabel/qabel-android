package de.qabel.qabelbox.identity.view.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.ui.DataViewAdapter

class IdentitiesAdapter(private val onClick: (identity: Identity) -> Unit,
                        private val onLongClick: (identity: Identity) -> Boolean) :
        RecyclerView.Adapter<IdentityListViewHolder>(), DataViewAdapter<Identity> {

    override var data: MutableList<Identity> = mutableListOf()

    override fun notifyView() {
        notifyDataSetChanged()
    }

    override fun notifyViewRange(start: Int, count: Int) {
        notifyItemRangeChanged(start, count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdentityListViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_identities, parent, false)
        return IdentityListViewHolder(v, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: IdentityListViewHolder, position: Int) {
        data.getOrNull(position)?.let {
            holder.bindTo(it)
        }
    }

    override fun getItemCount(): Int = data.size

}
