package de.qabel.qabelbox.ui.views

import android.content.Intent
import de.qabel.qabelbox.navigation.MainNavigator
import de.qabel.qabelbox.ui.AbstractUITest
import de.qabel.qabelbox.ui.idling.InjectedIdlingResource
import de.qabel.qabelbox.box.presenters.FileBrowserPresenter
import org.junit.Test

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.longClick
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers.*
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.box.views.FileBrowserFragment
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.helper.UIHelper
import de.qabel.qabelbox.ui.presenters.ChatPresenter
import org.hamcrest.core.IsNot
import org.junit.Ignore
import org.mockito.Mockito.*
import java.util.*

class FileBrowserFragmentTest: AbstractUITest() {


    lateinit var fragment: FileBrowserFragment
    lateinit var presenter: FileBrowserPresenter

    override fun setUp() {
        super.setUp()
    }

    override fun getDefaultIntent(): Intent {
        return Intent(mContext, MainActivity::class.java).apply {
            putExtra(MainActivity.START_FILES_FRAGMENT, true)
        }
    }

    fun launch() {
        launchActivity(defaultIntent)
        fragment = mActivity.fragmentManager.findFragmentByTag(
                MainNavigator.TAG_FILES_FRAGMENT) as FileBrowserFragment
        presenter = mock(FileBrowserPresenter::class.java)
        fragment.presenter = presenter

        val resource = InjectedIdlingResource()
        Espresso.registerIdlingResources(resource);
        fragment.setIdleCallback(resource)
    }


    @Test
    fun refreshActionBarButton() {
        launch()
        onView(withId(R.id.menu_refresh)).perform(click())
        verify(presenter).onRefresh()
    }

    @Test
    fun entryClick() {
        launch()
        val file = BrowserEntry.File("Name.txt", 42000, Date())
        val entries = listOf(file)
        fragment.showEntries(entries)

        onView(withText(file.name)).perform(click())
        verify(presenter).onClick(file)
    }

    @Test
    fun bottomSheetForFiles() {
        launch()
        val file = BrowserEntry.File("Name.txt", 42000, Date())
        fragment.showEntries(listOf(file))
        onView(withText(file.name)).perform(longClick())
        listOf(R.string.Open, R.string.Send, R.string.Delete, R.string.Export).forEach {
            onView(withText(it)).check(ViewAssertions.matches(isDisplayed()))
        }
    }

    @Test
    fun bottomSheetForFolders() {
        launch()
        val entry = BrowserEntry.Folder("FoobarFolder")
        fragment.showEntries(listOf(entry))
        onView(withText(entry.name)).perform(longClick())
        listOf(R.string.Delete).forEach {
            onView(withText(R.string.Delete)).check(ViewAssertions.matches(isDisplayed()))
        }
    }
}
