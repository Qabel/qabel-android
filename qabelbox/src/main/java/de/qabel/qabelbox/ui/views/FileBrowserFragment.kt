package de.qabel.qabelbox.ui.views

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import de.qabel.qabelbox.R
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.dagger.modules.FileBrowserModule
import de.qabel.qabelbox.dto.BrowserEntry
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.ui.adapters.FileAdapter
import de.qabel.qabelbox.ui.presenters.FileBrowserPresenter
import kotlinx.android.synthetic.main.fragment_files.*
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.onItemTouchListener
import javax.inject.Inject

class FileBrowserFragment: FileBrowserView, BaseFragment(), AnkoLogger,
        SwipeRefreshLayout.OnRefreshListener {

    companion object {
        fun newInstance() = FileBrowserFragment()
    }

    @Inject
    lateinit var presenter: FileBrowserPresenter

    lateinit var adapter: FileAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(MainActivityComponent::class.java)
                .plus(FileBrowserModule(this))
        component.inject(this)
        setHasOptionsMenu(true)
        adapter = FileAdapter(mutableListOf(), click = { presenter.onClick(it) })

        files_list.layoutManager = LinearLayoutManager(ctx)
        files_list.adapter = adapter
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_files, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun getTitle(): String? = ctx.getString(R.string.filebrowser)

    override fun showEntries(entries: List<BrowserEntry>) {
        onUiThread {
            adapter.entries.clear()
            adapter.entries.addAll(entries)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onRefresh() {
        presenter.onRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.ab_files, menu)
    }
}

