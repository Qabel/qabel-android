package de.qabel.qabelbox.ui.extensions

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import org.spongycastle.util.Pack

fun requestPermission(activity: Activity, permission: String, requestCode: Int) =
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)

fun isPermissionGranted(activity: Activity, permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

fun isPermissionGranted(permission: String, permissions: Array<out String>, results: IntArray): Boolean? {
    val index = permissions.indexOf(permission)
    if (index >= 0) {
        if (results[index] == PackageManager.PERMISSION_GRANTED) {
            return true
        } else if (results[index] == PackageManager.PERMISSION_DENIED) {
            return false
        }
    }
    return null
}
