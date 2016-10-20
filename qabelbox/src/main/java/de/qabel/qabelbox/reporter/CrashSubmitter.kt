package de.qabel.qabelbox.reporter

interface CrashSubmitter {
    fun submit(e: Throwable)
}

