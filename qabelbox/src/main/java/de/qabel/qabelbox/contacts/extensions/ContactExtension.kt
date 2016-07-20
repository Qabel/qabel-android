package de.qabel.qabelbox.contacts.extensions

import android.content.Context
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto

fun ContactDto.initials() = contact.alias.split(" ".toRegex()).map {
    it.first().toUpperCase()
}.joinToString("")

fun ContactDto.readableKey() = contact.keyIdentifier.mapIndexed { i, c ->
    val text = StringBuilder()
    text.append(c)
    if (i > 0) {
        val current = i.inc()
        if (current % 16 == 0) {
            text.appendln()
        } else if (current % 4 == 0) {
            text.append(" ")
        }
    }
    text.toString()
}.joinToString("")


fun ContactDto.readableUrl(): String {
    val dropUrlString = contact.dropUrls.first().toString();
    val last = dropUrlString.lastIndexOf("/").inc();
    val split = (last + (dropUrlString.length - last) / 2);
    return dropUrlString.substring((0 until last)) + "\n" +
            dropUrlString.substring((last until split)) + "\n" +
            dropUrlString.substring(split.inc())
}

fun ContactDto.contactColors(ctx: Context): List<Int> {
    val allColors = ctx.resources.obtainTypedArray(R.array.contact_colors);
    try {
        val colors = mutableListOf<Int>();
        val allLength = allColors.length();
        if (allLength == 0) return colors;

        val centerIndex = allLength / 2;

        identities.map { identity ->
            var colorIndex = identity.id.mod(allLength)
            if (colorIndex > 1) colorIndex /= 2;
            colors.add(allColors.getColor(centerIndex + if (identity.id % 2 == 0) colorIndex else -colorIndex, 0))
        }
        return colors.toList();
    } finally {
        allColors.recycle()
    }
}

fun List<Identity>.contains(keyIdentifier: String) = this.all { identity -> !identity.keyIdentifier.equals(keyIdentifier) }

