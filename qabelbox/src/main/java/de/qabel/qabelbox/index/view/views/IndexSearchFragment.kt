package de.qabel.qabelbox.index.view.views

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import de.qabel.qabelbox.fragments.BaseFragment
import org.jetbrains.anko.AnkoLogger
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.adapters.ContactsAdapter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.helper.UIHelper
import de.qabel.qabelbox.index.dagger.IndexSearchModule
import de.qabel.qabelbox.index.view.presenters.IndexSearchPresenter
import de.qabel.qabelbox.navigation.Navigator
import kotlinx.android.synthetic.main.fragment_contacts.*
import org.jetbrains.anko.ctx
import org.jetbrains.anko.longToast
import org.jetbrains.anko.onUiThread
import javax.inject.Inject

class IndexSearchFragment(): IndexSearchView, BaseFragment(),
        AnkoLogger, SearchView.OnQueryTextListener {

    override var searchString: String?
        get() = contact_search.query?.toString()
        set(value) {
            onUiThread { contact_search.setQuery(value, false) }
        }

    override val title: String by lazy { ctx.getString(R.string.index_search) }
    override val isFabNeeded = false

    @Inject
    lateinit var presenter: IndexSearchPresenter
    @Inject
    lateinit var navigator: Navigator

    val adapter = ContactsAdapter({ contact ->
        presenter.showDetails(contact)
    }, { contact ->
        presenter.showDetails(contact)
        true
    })

    override fun loadData(data: List<ContactDto>) {
        onUiThread {
            adapter.refresh(data)
            adapter.notifyDataSetChanged()
        }
    }

    override fun showEmpty() {
        onUiThread {
            adapter.refresh(emptyList())
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        UIHelper.hideKeyboard(activity, view)
        presenter.search()
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return true
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_contacts, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(MainActivityComponent::class.java).plus(IndexSearchModule(this))
        component.inject(this)

        setHasOptionsMenu(false)
        configureAsMainFragment()

        contact_list.layoutManager = LinearLayoutManager(view.context)
        contact_list.adapter = adapter
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contact_search.setOnQueryTextListener(this)
        contact_search.queryHint = getString(R.string.search)
    }

    override fun showError(error: Throwable) {
        onUiThread {
            longToast(error.message ?: "error")
            error("Error in IndexSearch", error)
        }
    }

    override fun showDetails(contactDto: ContactDto) {
        navigator.selectContactEdit(contactDto)
    }
}

