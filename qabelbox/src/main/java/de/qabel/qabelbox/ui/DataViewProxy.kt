package de.qabel.qabelbox.ui

import de.qabel.core.repository.framework.PagingResult
import rx.Observable

class DataViewProxy<in T>(private val loader: (offset: Int, pageSize: Int) -> Observable<PagingResult<T>>,
                          val dataView: DataFragment<T>,
                          private val pageSize: Int = 20) {

    private var loading = false
    private var total = 0
    private var loaded = -1

    fun loadMore() {
        if (!loading && total > loaded) {
            loadNext(loaded)
        }
    }

    fun load() {
        dataView.reset()
        loaded = 0
        total = -1
        loadNext(0)
    }

    private fun loadNext(offset: Int) {
        if (loading) return

        loading = true
        loader(offset, pageSize).subscribe({
            loading = false
            total = it.availableRange
            loaded += it.result.size
            dataView.prependData(it.result)
        })
    }

    fun canLoadMore() = loaded < total

}
