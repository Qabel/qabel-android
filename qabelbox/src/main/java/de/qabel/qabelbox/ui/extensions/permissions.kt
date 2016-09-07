package de.qabel.qabelbox.ui.extensions

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

fun requestPermission(activity: Activity, permission: String, requestCode: Int) =
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)

fun isPermissionGranted(activity: Activity, permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

fun isPermissionGranted(permission: String, permissions: Array<out String>, results: IntArray): Boolean {
    val index = permissions.indexOf(permission)
    if (index >= 0 && results[index] == PackageManager.PERMISSION_GRANTED) {
        return true
    }
    return false
}
