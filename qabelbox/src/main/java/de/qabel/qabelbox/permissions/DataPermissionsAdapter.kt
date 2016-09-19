package de.qabel.qabelbox.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import de.qabel.qabelbox.R
import de.qabel.qabelbox.permissions.isPermissionGranted
import de.qabel.qabelbox.permissions.requestPermission
import org.jetbrains.anko.alert

interface DataPermissionsAdapter {

    val permissionContext: Context

    fun hasPermission(permission: String) = isPermissionGranted(permissionContext, permission)

}

private fun DataPermissionsAdapter.showRequestPermissionDialog(activity: Activity,
                                                               requestCode: Int, permission: String,
                                                               textRes: Int, onDeny: () -> Unit) {
    permissionContext.alert(R.string.dialog_headline_info, textRes, {
        positiveButton(R.string.yes) {
            requestPermission(activity, permission, requestCode)
        }
        negativeButton(R.string.no) { onDeny() }
    }).show()
}

fun DataPermissionsAdapter.hasPhonePermission() =
        hasPermission(Manifest.permission.READ_PHONE_STATE)

fun DataPermissionsAdapter.requestPhonePermission(activity: Activity, requestCode: Int, onDeny: () -> Unit) =
        showRequestPermissionDialog(activity, requestCode, Manifest.permission.READ_PHONE_STATE,
                R.string.phone_number_request_info, onDeny)

fun DataPermissionsAdapter.hasContactsReadPermission() =
        hasPermission(Manifest.permission.READ_CONTACTS)

fun DataPermissionsAdapter.requestContactsReadPermission(activity: Activity, requestCode: Int, onDeny: () -> Unit) =
        showRequestPermissionDialog(activity, requestCode, Manifest.permission.READ_CONTACTS,
                R.string.phone_number_request_info, onDeny)

