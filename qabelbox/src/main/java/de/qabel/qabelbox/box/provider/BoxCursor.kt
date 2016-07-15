package de.qabel.qabelbox.box.provider

import android.database.MatrixCursor
import android.os.Bundle
import android.provider.DocumentsContract

internal class BoxCursor(columnNames: Array<String>) : MatrixCursor(columnNames) {

    var extraLoading: Boolean = false
    var error: String? = null

    override fun getExtras(): Bundle {

        val bundle = Bundle()
        bundle.putBoolean(DocumentsContract.EXTRA_LOADING, extraLoading)
        if (error != null) {
            bundle.putString(DocumentsContract.EXTRA_ERROR, error)
        }
        return bundle
    }
}
