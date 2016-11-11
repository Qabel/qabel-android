package de.qabel.qabelbox.ui.views

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.config.Identity
import de.qabel.core.config.Prefix
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.ACTIVE_IDENTITY
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.views.ExternalFileUploadActivity
import de.qabel.qabelbox.box.views.FolderChooserActivity
import de.qabel.qabelbox.ui.helper.UIBoxHelper
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExternalFileUploadActivityTest {


    @JvmField
    @Rule
    var activityTestRule: IntentsTestRule<ExternalFileUploadActivity> = IntentsTestRule(
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

    val mContext = InstrumentationRegistry.getTargetContext()!!

    val mBoxHelper = UIBoxHelper(mContext)

    @Before
    @Throws(Throwable::class)
    fun setUp() {
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
        // the startup sequenze doesn't use the mocked presenter
        mBoxHelper.removeAllIdentities()
        launch(listOf())
        assert(activity.isFinishing)
    }

    @Test
    fun choosesAlphabeticalFirstIdentity() {
        launch()
        activity.identity.alias shouldMatch equalTo(identities[1].alias)
    }

    @Test
    fun showDefaultPath() {
        launch()
        Page.folderButton.hasText("/Upload")
    }

    @Test
    fun startsFolderChooser() {
        launch()
        Page.startChooser()
        Intents.intended(Page.folderSelectIntentMatcher(secondIdentity))
    }

    @Test
    fun reactsToFolderChooserIntent() {
        launch()
        Intents.intending(Page.folderSelectIntentMatcher(secondIdentity)).respondWith(
                Instrumentation.ActivityResult(
                        Activity.RESULT_OK,
                            Intent().apply {
                                putExtra(FolderChooserActivity.FOLDER_DOCUMENT_ID,
                                        DocumentId(secondIdentity.keyIdentifier, secondIdentity.prefixes.first {
                                            p ->
                                            p.type == Prefix.TYPE.USER
                                        }.prefix, BoxPath.Root / "folder").toString())
                            }))


        Page.startChooser()
        assert(activity.path == BoxPath.Root / "folder")
        Page.folderButton.hasText("/folder")
    }

    fun ViewInteraction.hasText(text: String) {
        check(matches(withText(text)))
    }

    object Page {

        val folderButton: ViewInteraction
            get() = onView(withId(R.id.folderSelect))

        fun folderSelectIntentMatcher(identity: Identity) = Matchers.allOf(
                    IntentMatchers.toPackage("de.qabel.qabel.debug"),
                    IntentMatchers.hasExtra(ACTIVE_IDENTITY, identity.keyIdentifier))

        fun startChooser() {
            folderButton.perform(click())
        }
    }
}

