package de.qabel.qabelbox.ui

import de.qabel.core.logging.QabelLog

interface QblView : QabelLog {

    fun showDefaultError(throwable: Throwable)

}

