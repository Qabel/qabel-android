package de.qabel.desktop.repository.sqlite.fields


class ContactFields {

    companion object {
        const val ID = "id";
        const val ALIAS = "alias";
        const val PUBLIC_KEY = "publicKey";
        const val PHONE = "phone";
        const val EMAIL = "email";

        val ENTITY_FIELDS = listOf(ID, ALIAS, PUBLIC_KEY, PHONE, EMAIL)
    }

}
