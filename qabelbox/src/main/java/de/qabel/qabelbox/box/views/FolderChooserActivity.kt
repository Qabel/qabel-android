package de.qabel.qabelbox.box.views

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import kotlinx.android.synthetic.main.fragment_files.*
import org.jetbrains.anko.longToast

class FolderChooserActivity: CrashReportingActivity(), FolderChooserView, ActiveIdentityActivity,
QabelLog {

    //@Inject
    lateinit var presenter: FolderChooserPresenter

    override val activeIdentityKey: String?
        get() = intent.getStringExtra(ACTIVE_IDENTITY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.plus(ActivityModule(this)).plus(ActiveIdentityModule(this))
                .inject(this)
        setContentView(R.layout.fragment_files)
    }


    override fun finish(documentId: DocumentId) {
        setResult(CHOOSE_FOLDER, Intent().apply {
            putExtra(FOLDER_DOCUMENT_ID, documentId.toString())
        })
    }

    override fun showEntries(entries: List<BrowserEntry>) {
    }

    override fun refreshStart() {
        runOnUiThread {
            if (!swipeRefresh.isRefreshing) {
                swipeRefresh.isRefreshing = true
            }
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
        const val CHOOSE_FOLDER = 1
        const val FOLDER_DOCUMENT_ID = "FOLDER_DOCUMENT_ID"
    }

}

