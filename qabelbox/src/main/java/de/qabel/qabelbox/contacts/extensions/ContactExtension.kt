package de.qabel.qabelbox.contacts.extensions

import android.content.Context
import android.graphics.Color
import de.qabel.core.config.Entity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto

fun ContactDto.contactColors(ctx: Context): List<Int> = identities.map { it.color(ctx) }

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


