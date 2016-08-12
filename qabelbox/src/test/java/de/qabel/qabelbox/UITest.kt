package de.qabel.qabelbox

import android.content.Context


interface UITest {

    val context : Context

    fun getString(resId : Int) : String =
            context.getString(resId)

}
