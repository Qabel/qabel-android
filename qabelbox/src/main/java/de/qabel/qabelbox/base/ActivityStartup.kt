package de.qabel.qabelbox.base

interface ActivityStartup {
    /**
     * @return True, if the activity startup can be continued
     */
    fun onCreate(): Boolean
}

