package de.qabel.qabelbox.contacts.extensions

import android.content.Context
import android.graphics.Color
import de.qabel.core.config.Contact
import de.qabel.core.config.Entity
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto

fun Contact.displayName() : String {
    if(nickName != null && !nickName.isEmpty()){
        return nickName
    }
    return alias
}

fun Contact.initials() : String = displayName().split(" ".toRegex()).map {
    it.first().toUpperCase()
}.joinToString("")

fun ContactDto.initials() = contact.displayName().split(" ".toRegex()).map {
    it.first().toUpperCase()
}.joinToString("")

fun ContactDto.readableKey() = contact.keyIdentifier.foldIndexed(StringBuilder(), { i, text, char ->
    text.append(char)
    if (i > 0) {
        val current = i.inc()
        if (current % 16 == 0) {
            text.append("\n")
        } else if (current % 4 == 0) {
            text.append(" ")
        }
    }
    text
})


fun ContactDto.readableUrl(): String {
    val dropUrlString = contact.dropUrls.first().toString();
    val last = dropUrlString.lastIndexOf("/").inc();
    val split = (last + (dropUrlString.length - last) / 2);
    return dropUrlString.substring((0 until last)) + "\n" +
            dropUrlString.substring((last until split)) + "\n" +
            dropUrlString.substring(split.inc())
}

fun ContactDto.contactColors(ctx: Context): List<Int> = identities.map { it.color(ctx)  }

fun Entity.color(ctx: Context): Int {
    val allColors = ctx.resources.obtainTypedArray(R.array.contact_colors)
    try {
        val allLength = allColors.length()
        if (allLength == 0) {
            return Color.GRAY
        }
        val centerIndex = allLength / 2
        var colorIndex = Math.abs(keyIdentifier.hashCode()).mod(allLength)
        if (colorIndex > 1) colorIndex /= 2

        return allColors.getColor(centerIndex + if (id % 2 == 0) colorIndex else -colorIndex, 0)
    } finally {
        allColors.recycle()
    }
}
fun Identity.initials() = alias.split(" ".toRegex()).map {
    it.first().toUpperCase()
}.joinToString("")


