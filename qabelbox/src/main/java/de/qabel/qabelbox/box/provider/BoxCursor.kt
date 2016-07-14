package de.qabel.qabelbox.box.provider

import android.database.MatrixCursor
import android.os.Bundle
import android.provider.DocumentsContract

internal class BoxCursor(columnNames: Array<String>) : MatrixCursor(columnNames) {

    private var extraLoading: Boolean = false
    private var error: String? = null

    fun setExtraLoading(loading: Boolean) {

        this.extraLoading = loading
    }

    override fun getExtras(): Bundle {

        val bundle = Bundle()
        bundle.putBoolean(DocumentsContract.EXTRA_LOADING, extraLoading)
        if (error != null) {
            bundle.putString(DocumentsContract.EXTRA_ERROR, error)
        }
        return bundle
    }

    fun setError(error: String) {

        this.error = error
    }
}
