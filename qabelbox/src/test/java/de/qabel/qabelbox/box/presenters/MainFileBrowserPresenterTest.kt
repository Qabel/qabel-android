package de.qabel.qabelbox.box.presenters

import com.nhaarman.mockito_kotlin.*
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.BrowserEntry.File
import de.qabel.qabelbox.box.interactor.FileBrowserUseCase
import de.qabel.qabelbox.box.views.FileBrowserView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.lang.kotlin.toSingletonObservable
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class MainFileBrowserPresenterTest {

    val view: FileBrowserView = mock()
    val useCase: FileBrowserUseCase = mock()
    val presenter = MainFileBrowserPresenter(view, useCase)
    val sample = File("foobar.txt", 42000, Date())
    val sampleFiles = listOf(sample)

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
        stubWith(emptyList())
        whenever(useCase.delete(any())).thenReturn(Unit.toSingletonObservable())
        presenter.deleteFolder(BrowserEntry.Folder("folder"))
        verify(view).showEntries(emptyList())
        verify(useCase).delete(eq(BoxPath.Root / "folder"))
    }

    @Test
    fun browseToFolder() {
        stubWith(sampleFiles, path = BoxPath.Root / "folder")
        presenter.onClick(BrowserEntry.Folder("folder"))
        verify(view).showEntries(sampleFiles)
    }

}
