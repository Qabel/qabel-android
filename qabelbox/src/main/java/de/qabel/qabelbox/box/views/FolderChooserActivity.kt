package de.qabel.qabelbox.box.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.ACTIVE_IDENTITY
import de.qabel.qabelbox.base.ActiveIdentityActivity
import de.qabel.qabelbox.base.CrashReportingActivity
import de.qabel.qabelbox.box.adapters.FileAdapter
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.presenters.FolderChooserPresenter
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.dagger.modules.ActiveIdentityModule
import de.qabel.qabelbox.dagger.modules.ActivityModule
import de.qabel.qabelbox.dagger.modules.FolderChooserModule
import kotlinx.android.synthetic.main.activity_folder_chooser.*
import kotlinx.android.synthetic.main.fragment_files.*
import org.jetbrains.anko.ctx
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

class FolderChooserActivity: CrashReportingActivity(), FolderChooserView, ActiveIdentityActivity,
QabelLog {

    @Inject
    lateinit var presenter: FolderChooserPresenter

    lateinit var adapter: FileAdapter

    override val activeIdentityKey: String?
        get() = intent.getStringExtra(ACTIVE_IDENTITY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.plus(ActivityModule(this))
                .plus(ActiveIdentityModule(this))
                .plus(FolderChooserModule((this)))
                .inject(this)
        setContentView(R.layout.activity_folder_chooser)
        setSupportActionBar(toolbar)
        adapter = FileAdapter(mutableListOf(), click = {
                    if (it is BrowserEntry.Folder) {
                        presenter.enter(it)
                    }
                })
        files_list.layoutManager = LinearLayoutManager(ctx)
        files_list.adapter = adapter
        swipeRefresh.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        if (!intent.getBooleanExtra(TEST_RUN, false)) {
            presenter.onRefresh()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.ab_folder_chooser, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_up -> presenter.navigateUp()
            R.id.select_folder -> presenter.selectFolder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun finish(documentId: DocumentId) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(FOLDER_DOCUMENT_ID, documentId.toString())
        })
        finish()
    }

    override fun showEntries(entries: List<BrowserEntry>) {
        runOnUiThread {
            adapter.entries.clear()
            if (entries.size > 0) {
                empty_view.visibility = View.INVISIBLE
            } else {
                empty_view.visibility = View.VISIBLE
            }
            adapter.entries.addAll(entries)
            adapter.notifyDataSetChanged()
        }
    }

    override fun refreshStart() {
        runOnUiThread {
            if (!swipeRefresh.isRefreshing) {
                swipeRefresh.isRefreshing = true
            }
        }
    }

    override fun backgroundRefreshStart() {
        runOnUiThread {
            background_progress_bar?.visibility = View.VISIBLE
        }
    }

    override fun backgroundRefreshDone() {
        runOnUiThread {
            background_progress_bar?.visibility = View.INVISIBLE
        }
    }

    override fun refreshDone() {
        runOnUiThread {
            swipeRefresh.isRefreshing = false
        }
    }

    override fun showError(throwable: Throwable) {
        runOnUiThread {
            longToast(throwable.message ?: "Error")
            error("Error", throwable)
            refreshDone()
        }
    }

    override fun onBackPressed() {
        if (presenter.navigateUp()) {
            finish()
        }
    }

    companion object {
        const val FOLDER_DOCUMENT_ID = "FOLDER_DOCUMENT_ID"
        const val TEST_RUN = "TEST_RUN"
    }

}

