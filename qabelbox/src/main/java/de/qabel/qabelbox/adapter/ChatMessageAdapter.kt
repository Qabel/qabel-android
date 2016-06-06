package de.qabel.qabelbox.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import de.qabel.qabelbox.dto.ChatMessage

class ChatMessageAdapter(): BaseAdapter() {
    var chatMessages: List<ChatMessage> = listOf()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        throw UnsupportedOperationException()
    }

    override fun getItem(position: Int): Any? {
        throw UnsupportedOperationException()
    }

    override fun getItemId(position: Int): Long {
        throw UnsupportedOperationException()
    }

    override fun getCount(): Int {
        throw UnsupportedOperationException()
    }

}
