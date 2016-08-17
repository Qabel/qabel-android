package de.qabel.qabelbox.ui


interface DataFragment<in T> {

    fun reset()
    fun appendData(model : List<T>)
    fun prependData(models : List<T>)

}
