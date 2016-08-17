package de.qabel.qabelbox.ui


interface DataViewAdapter<T> {

    var data: List<T>

    fun init(data: List<T>) {
        this.data = data
        notifyView()
    }

    fun reset() {
        data = emptyList()
        notifyView()
    }

    fun append(model: List<T>) {
        data = data.plus(model)
        notifyView()
    }

    fun prepend(models: List<T>) {
        data = models.plus(data)
        notifyView()
    }

    fun notifyView()

    fun notifyViewRange(start: Int, count: Int)
}
