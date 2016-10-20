package de.qabel.qabelbox.ui.extensions

import android.text.InputType
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.BaseFragment
import de.qabel.qabelbox.helper.UIHelper
import org.jetbrains.anko.*

internal fun BaseFragment.showMessage(title: Int, message: Int) {
    UIHelper.showDialogMessage(activity, title, message);
}

internal fun BaseFragment.showMessage(title: Int, message: Int, vararg messageParams: Any?) {
    UIHelper.showDialogMessage(activity, title, activity.getString(message, *messageParams));
}

internal fun BaseFragment.showQuantityMessage(title: Int, message: Int, quantity: Int, vararg messageParams: Any?) {
    UIHelper.showDialogMessage(activity, title, activity.resources.getQuantityString(message, quantity, *messageParams));
}

internal fun BaseFragment.showConfirmation(title: Int, message: Int, params: Any, yesClick: () -> Unit) {
    UIHelper.showDialogMessage(activity, title, activity.getString(message, params),
            { dialogInterface, i -> yesClick() });
}

internal fun BaseFragment.showEnterTextDialog(headerText: Int, hintText: Int,
                                              textInputType: Int = InputType.TYPE_CLASS_TEXT,
                                              onSubmit: (text: String) -> Unit,
                                              currentValue: CharSequence = "",
                                              helpText: Int? = null) {
    UI {
        alert(headerText) {
            customView {
                verticalLayout {
                    val editText = editText {
                        inputType = textInputType
                        hint = ctx.getString(hintText)
                        setText(currentValue)
                    }.lparams {
                        width = matchParent
                        horizontalMargin = dip(20)
                    }
                    helpText?.let {
                        textView {
                            text = getString(helpText)
                        }.lparams {
                            width = matchParent
                            horizontalMargin = dip(25)
                        }
                    }
                    positiveButton(R.string.ok) {
                        onSubmit(editText.text.toString())
                    }
                    negativeButton(R.string.cancel) { }
                }
            }
        }.show()
    }
}
