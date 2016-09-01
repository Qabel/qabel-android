package de.qabel.qabelbox.ui

import de.qabel.core.util.QblLogger

interface QblView : QblLogger {

    fun showDefaultError(throwable: Throwable)

}

