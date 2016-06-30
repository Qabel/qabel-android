package de.qabel.qabelbox.ui.views

import android.support.test.espresso.Espresso
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.navigation.MainNavigator
import de.qabel.qabelbox.ui.AbstractUITest
import de.qabel.qabelbox.ui.idling.InjectedIdlingResource
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Assert.*
import org.mockito.Matchers.*
import org.mockito.Mockito.*
import org.hamcrest.Matchers.*

import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

class FileBrowserFragmentTest: AbstractUITest() {


    val contact = IdentityHelper.createContact("contact")
    lateinit var fragment: FileBrowserFragment

    override fun setUp() {
        super.setUp()
        contactRepository.save(contact, identity)
    }

    fun launch() {
        with(defaultIntent) {
            launchActivity(this)
        }
        fragment = mActivity.fragmentManager.findFragmentByTag(
                MainNavigator.TAG_FILES_FRAGMENT) as FileBrowserFragment

        val resource = InjectedIdlingResource()
        Espresso.registerIdlingResources(resource);
        fragment.setIdleCallback(resource)
    }


    @Test
    fun testShowEmptyFileBrowser() {
        launch()
    }
}
