package de.qabel.qabelbox.ui.views

import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.ACTIVE_IDENTITY
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.presenters.FolderChooserPresenter
import de.qabel.qabelbox.box.views.FolderChooserActivity
import de.qabel.qabelbox.ui.helper.UIBoxHelper
import android.support.test.espresso.assertion.ViewAssertions.matches
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify

class FolderChooserActivityTest {

    @JvmField
    @Rule
    var activityTestRule: ActivityTestRule<FolderChooserActivity> = ActivityTestRule(
            FolderChooserActivity::class.java, false, false)

    lateinit var identity: Identity

    var presenter: FolderChooserPresenter = Mockito.mock(FolderChooserPresenter::class.java)
    val folder = BrowserEntry.Folder("folder")

    val defaultIntent: Intent
        get() =  Intent(InstrumentationRegistry.getTargetContext(), FolderChooserActivity::class.java).apply {
            putExtra(ACTIVE_IDENTITY, identity.keyIdentifier)
            putExtra(FolderChooserActivity.TEST_RUN, true)
        }

    @Before
    fun setUp() {
        val mContext = InstrumentationRegistry.getTargetContext()

        val mBoxHelper = UIBoxHelper(mContext)
        mBoxHelper.createTokenIfNeeded(false)
        mBoxHelper.removeAllIdentities()
        identity = mBoxHelper.addIdentity("spoon123")

    }
    fun launch() {
        activityTestRule.launchActivity(defaultIntent)
        activityTestRule.activity.presenter = presenter
    }

    @Test
    fun entriesVisible() {
        launch()
        activityTestRule.activity.showEntries(listOf(folder))
        onView(withText(folder.name)).check(matches(isDisplayed()))
    }


    @Test
    fun entryClick() {
        launch()
        activityTestRule.activity.showEntries(listOf(folder))
        onView(withText(folder.name)).perform(click())
        verify(presenter).enter(folder)
    }

    @Test
    fun selectFolder() {
        launch()
        onView(withId(R.id.select_folder)).perform(click())
        verify(presenter).selectFolder()
    }

    @Test
    fun navigateUp() {
        launch()
        onView(withId(R.id.menu_up)).perform(click())
        verify(presenter).navigateUp()
    }

}
