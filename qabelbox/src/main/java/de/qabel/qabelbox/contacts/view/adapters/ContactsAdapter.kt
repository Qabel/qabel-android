package de.qabel.qabelbox.contacts.view.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.qabel.core.ui.displayName
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.adapters.ContactsViewHolder
import java.util.*

class ContactsAdapter(val clickListener: (ContactDto) -> Unit,
                      val longClickListener: (ContactDto) -> Boolean) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var TYPE_CONTACT = 1;

    interface ListItem {
        val type: Int
    }

    data class ContactListItem(val contact: ContactDto) : ListItem {
        override val type: Int get() = 1;
    }

    data class HeaderListItem(val header: Char) : ListItem {
        override val type: Int get() = 0;
    }

    private var listItems = mutableListOf<ListItem>();
    private var sectionCount = 0;

    fun refresh(data: List<ContactDto>) {
        var sectionName: Char? = null
        sectionCount = 0
        listItems.clear()
        val sorted = data.toSortedSet(Comparator { t, t2 ->
            t.contact.displayName().compareTo(t2.contact.displayName(), true) })

        sorted.forEach { contact ->
            val section = contact.contact.displayName().first().toUpperCase()
            if (sectionName == null || section != sectionName) {
                listItems.add(HeaderListItem(section))
                sectionName = section
                sectionCount.inc()
            }
            listItems.add(ContactListItem(contact))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return listItems[position].type;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_CONTACT -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contacts, parent, false)
                return ContactsViewHolder(v, clickListener, longClickListener);
            }
            else -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.list_header, parent, false)
                return ContactsViewHolder.ContactHeaderViewHolder(v);
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ContactsViewHolder -> holder.bindTo((listItems[position] as ContactListItem).contact)
            is ContactsViewHolder.ContactHeaderViewHolder -> holder.bindTo((listItems[position] as HeaderListItem).header)
        }
    }

    fun getContactCount(): Int {
        return itemCount - sectionCount;
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

}
