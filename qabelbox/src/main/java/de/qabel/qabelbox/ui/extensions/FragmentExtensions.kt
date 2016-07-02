package de.qabel.qabelbox.ui.extensions

import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.helper.UIHelper


internal fun BaseFragment.showMessage(title: Int, message: Int) {
    UIHelper.showDialogMessage(activity, title, message);
}

internal fun BaseFragment.showMessage(title: Int, message: Int, vararg messageParams : Any?) {
    UIHelper.showDialogMessage(activity, title, activity.getString(message, *messageParams));
}

internal fun BaseFragment.showQuantityMessage(title: Int, message: Int, quantity: Int, vararg messageParams: Any?) {
    UIHelper.showDialogMessage(activity, title, activity.resources.getQuantityString(message, quantity, *messageParams));
}

internal fun BaseFragment.showConfirmation(title: Int, message: Int, params: Any, yesClick: () -> Unit) {
    UIHelper.showDialogMessage(activity, title, activity.getString(message, params),
            { dialogInterface, i -> yesClick() });
}
