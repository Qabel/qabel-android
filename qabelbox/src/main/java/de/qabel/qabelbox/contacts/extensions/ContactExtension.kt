package de.qabel.qabelbox.contacts.extensions

import android.content.Context
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto

fun ContactDto.initials(): String {
    val names = contact.alias.split(" ".toRegex());
    var result = StringBuilder();
    (0 until names.size).map {
        result.append(names[it].first().toUpperCase());
    }
    return result.toString();
}

fun ContactDto.readableKey(): String {
    val builder = StringBuilder();
    val publicKeyString = contact.ecPublicKey.readableKeyIdentifier;
    (0 until publicKeyString.length).forEach {
        builder.append(publicKeyString[it]);
        if (it > 0) {
            val current = it.inc();
            if (current % 16 == 0) {
                builder.append("\n");
            } else if (current % 4 == 0) {
                builder.append(" ");
            }
        }
    }
    return builder.toString();
}

fun ContactDto.readableUrl(): String {
    val dropUrlString = contact.dropUrls.first().toString();
    val last = dropUrlString.lastIndexOf("/").inc();
    return dropUrlString.substring((0 until last)) + "\n" + dropUrlString.substring(last);
}

fun ContactDto.contactColors(ctx: Context): List<Int> {
    val allColors = ctx.resources.getIntArray(R.array.contact_colors);
    val colors = mutableListOf<Int>();
    identities.map { identity ->
        var colorIndex = identity.id % allColors.size / 2;
        var centerIndex = allColors.size / 2;
        if (identity.id % 2 == 0) {
            colors.add(allColors[centerIndex + colorIndex])
        } else {
            colors.add(allColors[centerIndex - colorIndex])
        }
    }
    return colors.toList();
}

