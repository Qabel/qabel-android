package de.qabel.qabelbox.box.presenters

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.box.storage.dto.BoxPath
import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.BrowserEntry
import de.qabel.client.box.interactor.ReadFileBrowser
import de.qabel.qabelbox.box.views.FolderChooserView
import de.qabel.qabelbox.eq
import org.junit.Before
import org.junit.Test
import rx.lang.kotlin.toSingletonObservable

class MainFolderChooserPresenterTest {

    val view: FolderChooserView = mock()
    val useCase: ReadFileBrowser = mock()
    lateinit var presenter: MainFolderChooserPresenter
    val navigatingPresenter: NavigatingPresenter = object : MainNavigatingPresenter(view, useCase) {
        override fun onRefresh() {
        }

        override fun navigateUp() = true
    }
    val sample = BrowserEntry.Folder("foobar")
    val sampleFiles = listOf(sample)
    val folder = BoxPath.Root / "foobar"

    @Before
    fun setUp() {
        presenter = MainFolderChooserPresenter(view, useCase, navigatingPresenter)
    }

    fun stubWith(sample: List<BrowserEntry>, path: BoxPath.FolderLike = BoxPath.Root) {
        whenever(useCase.list(path)).thenReturn(sample.toSingletonObservable())
    }

    @Test
    fun enter() {
        stubWith(sampleFiles, folder)
        presenter.enter(sample)
        presenter.path eq folder
    }

    @Test
    fun selectFolder() {
        val documentId: DocumentId = DocumentId("key", "prefix", folder)
        whenever(useCase.asDocumentId(folder)).thenReturn(documentId.toSingletonObservable())
        presenter.path = folder
        presenter.selectFolder()
        verify(view).finish(documentId)
    }

}
