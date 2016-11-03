package de.qabel.qabelbox.box.views

import android.os.Bundle
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.base.CrashReportingActivity
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.presenters.FolderChooserPresenter
import de.qabel.qabelbox.box.provider.DocumentId
import javax.inject.Inject

class FolderChooserActivity: CrashReportingActivity(), FolderChooserView, QabelLog {

    @Inject
    lateinit var presenter: FolderChooserPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun finish(documentId: DocumentId) {
    }

    override fun showEntries(entries: List<BrowserEntry>) {
    }

    override fun refreshDone() {
    }

    override fun refreshStart() {
    }

    override fun showError(throwable: Throwable) {
    }

}

