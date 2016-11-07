package de.qabel.qabelbox.box.presenters

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.inmemory.InMemoryIdentityRepository
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.interactor.FileBrowser
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.views.FileUploadView
import de.qabel.qabelbox.box.views.FolderChooserView
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor
import org.junit.Before
import org.junit.Test
import rx.lang.kotlin.toSingletonObservable

class ExternalFileUploadPresenterTest {

    val identityInteractor: ReadOnlyIdentityInteractor = mock()
    val view: FileUploadView = mock()
    val presenter = ExternalFileUploadPresenter(view, identityInteractor)

    @Test
    fun startUpload() {

    }

}
