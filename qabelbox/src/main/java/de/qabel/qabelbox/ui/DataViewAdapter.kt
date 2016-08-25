package de.qabel.qabelbox.ui


interface DataViewAdapter<T> {

    var data: MutableList<T>

    fun init(data: List<T>) {
        this.data = data.toMutableList()
        notifyView()
    }

    fun reset() {
        data = mutableListOf()
        notifyView()
    }

    fun append(model: List<T>) {
        data.addAll(model)
        notifyView()
    }

    fun prepend(models: List<T>) {
        data.addAll(0, models)
        notifyView()
    }

    fun notifyView()

    fun notifyViewRange(start: Int, count: Int)
}
