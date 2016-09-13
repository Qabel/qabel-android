package de.qabel.qabelbox.ui

import de.qabel.core.logging.QblLogger

interface QblView : QblLogger {

    fun showDefaultError(throwable: Throwable)

}

