package de.qabel.qabelbox.ui

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.OnScrollListener
import de.qabel.core.ui.DataViewProxy

/**
 * TODO In development
 */
class EndlessScrollRecyclerViewListener(private val loader: DataViewProxy<*>) : OnScrollListener() {

    private val visibleThreshold = 3 // The minimum amount of items to have above your current scroll position before loading more.

    private var previousTotal = 0 // The total number of items in the dataset after the last load
    private var loading = true // True if we are still waiting for the last set of data to load.

    private var firstVisibleItem: Int = 0
    private var visibleItemCount: Int = 0
    private var totalItemCount: Int = 0

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        visibleItemCount = layoutManager.childCount
        totalItemCount = layoutManager.itemCount
        firstVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()

        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false
                previousTotal = totalItemCount
            }
        } else if (totalItemCount < firstVisibleItem + visibleThreshold) {
            loader.loadMore()
            loading = true
        }
    }

}
