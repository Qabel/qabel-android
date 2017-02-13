package de.qabel.qabelbox.box.presenters

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.*
import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.views.FileBrowserView
import org.junit.Before
import org.junit.Test
import rx.Observable
import rx.lang.kotlin.toSingletonObservable
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*

class MainReadFileBrowserPresenterTest {

 /*   val view: FileBrowserView = mock()
    val useCase: ReadFileBrowser = mock()
    lateinit var presenter: MainFileBrowserPresenter
    val sample = File("foobar.txt", 42000, Date())
    val sampleFiles = listOf(sample)

    @Before
    fun setUp() {
         presenter = MainFileBrowserPresenter(view, useCase, mock(), mock(), mock())
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
    fun refreshError() {
        val exception = FileNotFoundException("test")
        whenever(useCase.list(any())).thenReturn(Observable.error(exception))

        presenter.onRefresh()

        verify(view).showError(exception)
    }

    @Test
    fun delete() {
        stubWith(emptyList())
        whenever(useCase.delete(any())).thenReturn(Unit.toSingletonObservable())
        presenter.delete(sample)
        verify(view).showEntries(emptyList())
        verify(useCase).delete(eq(BoxPath.Root * sample.name))

        presenter.path = BoxPath.Root / "folder"
        presenter.delete(sample)
        verify(useCase).delete(eq(BoxPath.Root /"folder"/sample.name))
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
    fun deleteFolderError() {
        val exception = FileNotFoundException("test")
        whenever(useCase.delete(any())).thenReturn(Observable.error(exception))

        presenter.deleteFolder(BrowserEntry.Folder("folder"))

        verify(view).showError(exception)
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
    fun createFolderError() {
        val exception = FileNotFoundException("test")
        whenever(useCase.createFolder(any())).thenReturn(Observable.error(exception))

        presenter.createFolder(BrowserEntry.Folder("folder"))

        verify(view).showError(exception)
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

        presenter.navigateUp() shouldMatch equalTo(true)
        verify(view).showEntries(sampleFiles)

        presenter.navigateUp() shouldMatch equalTo(false)
    }*/

}
