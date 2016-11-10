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
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.views.ExternalFileUploadActivity
import kotlinx.android.synthetic.main.activity_external_upload.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class ExternalFileUploadActivityTest {


    @JvmField
    @Rule
    var activityTestRule: ActivityTestRule<ExternalFileUploadActivity> = ActivityTestRule(
            ExternalFileUploadActivity::class.java, false, false)
    val activity: ExternalFileUploadActivity
        get() = activityTestRule.activity

    lateinit var identity: Identity
    lateinit var secondIdentity: Identity

    lateinit var identities: List<FileUploadPresenter.IdentitySelection>
    val folder = BrowserEntry.Folder("folder")

    val defaultIntent: Intent
        get() = Intent(InstrumentationRegistry.getTargetContext(), FolderChooserActivity::class.java)

    open class Presenter(override val availableIdentities: List<FileUploadPresenter.IdentitySelection>)
    : FileUploadPresenter {
        override val defaultPath: BoxPath = BoxPath.Root * "public"

        override fun confirm() { }
    }


    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val mContext = InstrumentationRegistry.getTargetContext()

        val mBoxHelper = UIBoxHelper(mContext)
        mBoxHelper.createTokenIfNeeded(false)
        mBoxHelper.removeAllIdentities()
        identity = mBoxHelper.addIdentity("spoon123")
        secondIdentity = mBoxHelper.addIdentity("second")
        identities = listOf(
            FileUploadPresenter.IdentitySelection(identity),
            FileUploadPresenter.IdentitySelection(secondIdentity))
    }

    fun launch(identities: List<FileUploadPresenter.IdentitySelection>? = null): Presenter {
        activityTestRule.launchActivity(defaultIntent)
        val presenter = Presenter(identities ?: listOf())
        activityTestRule.activity.presenter = presenter
        return presenter
    }

    @Test
    fun finishesWithoutIdentities() {
        launch(listOf())
        //activity.folderSelect.text.toString() shouldMatch equalTo("/Upload")
        assert(activity.isFinishing)
        //onView(withId(R.id.folderSelect)).check(matches(withText("/Upload")))
    }

    @Test
    fun choosesFirstIdentity() {
        launch()
        activity.identity.alias shouldMatch equalTo(identities[0].alias)
    }
}

