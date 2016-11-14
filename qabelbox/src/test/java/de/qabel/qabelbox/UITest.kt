package de.qabel.qabelbox

import android.content.Context


interface UITest {

    val context : Context

    fun getString(resId : Int) : String =
            context.getString(resId)

    fun getString(resId : Int, vararg args: Any) : String = context.getString(resId, *args)

}
