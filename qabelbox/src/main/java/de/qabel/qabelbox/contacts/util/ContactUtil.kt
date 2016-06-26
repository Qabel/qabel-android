package de.qabel.qabelbox.contacts.util

open class ContactUtil {

    fun getInitials(contactAlias: String): String {
        val names = contactAlias.split(" ".toRegex());
        var result = StringBuilder();
        (0 until names.size).map {
            result.append(names[it].first().toUpperCase());
        }
        return result.toString();
    }

    fun getReadableKey(publicKey: String): String {
        var builder = StringBuilder();
        (0 until publicKey.length).forEach {
            builder.append(publicKey[it]);
            if (it > 0) {
                var current = it.inc();
                if (current % 16 == 0) {
                    builder.append("\n");
                } else if (current % 4 == 0) {
                    builder.append(" ");
                }
            }
        }
        return builder.toString();
    }

    fun getReadableUrl(dropUrl: String): String {
        var last = dropUrl.lastIndexOf("/").inc();
        return dropUrl.substring((0 until last)) + "\n" + dropUrl.substring(last);
    }

}
