package de.qabel.qabelbox.box.presenters

import com.nhaarman.mockito_kotlin.*
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.BrowserEntry.File
import de.qabel.qabelbox.box.dto.DownloadSource
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.interactor.FileBrowser
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.views.FileBrowserView
import de.qabel.qabelbox.util.toDownloadSource
import org.junit.Before
import org.junit.Test
import rx.lang.kotlin.toSingletonObservable
import java.io.InputStream
import java.util.*

class MainFileBrowserPresenterTest {

    val view: FileBrowserView = mock()
    val useCase: FileBrowser = mock()
    lateinit var presenter: MainFileBrowserPresenter
    val sample = File("foobar.txt", 42000, Date())
    val sampleFiles = listOf(sample)

    @Before
    fun setUp() {
         presenter = MainFileBrowserPresenter(view, useCase)
    }

    fun stubWith(sample: List<BrowserEntry>, path: BoxPath.FolderLike = BoxPath.Root) {
        whenever(useCase.list(path)).thenReturn(sample.toSingletonObservable())
    }

    @Test
    fun refresh() {
        stubWith(sampleFiles)
        presenter.onRefresh()
        verify(view).showEntries(sampleFiles)
    }

    @Test
    fun delete() {
        stubWith(emptyList())
        whenever(useCase.delete(any())).thenReturn(Unit.toSingletonObservable())
        presenter.delete(sample)
        verify(view).showEntries(emptyList())
        verify(useCase).delete(eq(BoxPath.Root * sample.name))
    }

    @Test
    fun deleteFolder() {
        stubWith(sampleFiles, path = BoxPath.Root/"folder")
        whenever(useCase.delete(any())).thenReturn(Unit.toSingletonObservable())
        presenter.path = BoxPath.Root / "folder"

        presenter.deleteFolder(BrowserEntry.Folder("innerFolder"))

        verify(view).showEntries(sampleFiles)
        verify(useCase).delete(eq(BoxPath.Root /"folder"/"innerFolder"))
    }

    @Test
    fun browseToFolder() {
        stubWith(sampleFiles, path = BoxPath.Root / "folder")
        presenter.onClick(BrowserEntry.Folder("folder"))
        verify(view).showEntries(sampleFiles)
    }

    @Test
    fun createFolder() {
        val list = listOf(BrowserEntry.Folder("folder"))
        stubWith(list)
        whenever(useCase.createFolder(BoxPath.Root / "folder"))
                .thenReturn(Unit.toSingletonObservable())

        presenter.createFolder(BrowserEntry.Folder("folder"))

        verify(useCase).createFolder(BoxPath.Root / "folder")
        verify(view).showEntries(list)
    }

    @Test
    fun upload() {
        stubWith(sampleFiles)
        whenever(useCase.upload(any(), any())).thenReturn(Unit.toSingletonObservable())
        val stream: InputStream = mock()
        val size = 42L
        val mTime: Date = mock()

        val file = File("foo.txt", size, mTime)
        presenter.upload(file, stream)

        verify(useCase).upload(BoxPath.Root * "foo.txt", UploadSource(stream, file))
        verify(view).showEntries(sampleFiles)
    }

    @Test
    fun open() {
        val docId = DocumentId("foo", "bar", BoxPath.Root * sample.name)
        whenever(useCase.asDocumentId(docId.path)).thenReturn(docId.toSingletonObservable())
        presenter.onClick(sample)

        verify(view).open(docId)
    }

    @Test
    fun export() {
        val docId = DocumentId("foo", "bar", BoxPath.Root * sample.name)
        whenever(useCase.asDocumentId(docId.path)).thenReturn(docId.toSingletonObservable())
        presenter.export(sample)

        verify(view).export(docId)
    }

    @Test
    fun share() {
        val docId = DocumentId("foo", "bar", BoxPath.Root * sample.name)
        whenever(useCase.asDocumentId(docId.path)).thenReturn(docId.toSingletonObservable())
        presenter.share(sample)

        verify(view).share(docId)
    }

    @Test
    fun navigateUp() {
        stubWith(sampleFiles)
        presenter.path = BoxPath.Root / "folder"

        presenter.navigateUp()
        verify(view).showEntries(sampleFiles)
    }

}
