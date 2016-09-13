package de.qabel.qabelbox.ui.extensions

import android.view.View
import android.widget.TextView

fun TextView.setOrGone(valueText: String) {
    if (valueText.isEmpty()) {
        visibility = View.GONE
    } else {
        text = valueText
        visibility = View.VISIBLE
    }
}
