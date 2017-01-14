package de.qabel.qabelbox.ui.views

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.box.storage.dto.BoxPath
import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.BrowserEntry
import de.qabel.core.config.Identity
import de.qabel.core.config.Prefix
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.ACTIVE_IDENTITY
import de.qabel.qabelbox.box.interactor.BoxServiceStarter
import de.qabel.qabelbox.contacts.dto.EntitySelection
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.views.ExternalFileUploadActivity
import de.qabel.qabelbox.box.views.FolderChooserActivity
import de.qabel.qabelbox.ui.helper.SystemAnimations
import de.qabel.qabelbox.ui.helper.UIActionHelper
import de.qabel.qabelbox.ui.helper.UIBoxHelper
import de.qabel.qabelbox.ui.helper.UITestHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class ExternalFileUploadActivityTest {


    @JvmField
    @Rule
    var activityTestRule: IntentsTestRule<ExternalFileUploadActivity> = IntentsTestRule(
            ExternalFileUploadActivity::class.java, false, false)
    val activity: ExternalFileUploadActivity
        get() = activityTestRule.activity

    private var wakeLock: PowerManager.WakeLock? = null
    private var mSystemAnimations: SystemAnimations? = null

    lateinit var identity: Identity
    lateinit var secondIdentity: Identity

    lateinit var identities: List<EntitySelection>
    val folder = BrowserEntry.Folder("folder")

    val uri: Uri = Uri.fromFile(createTempFile())
    val defaultIntent: Intent
        get() {
            return Intent(InstrumentationRegistry.getTargetContext(), FolderChooserActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(ExternalFileUploadActivity.TEST_RUN, true)
            }
        }

    open class Presenter(override val availableIdentities: List<EntitySelection>)
    : FileUploadPresenter {
        var confirmed = false
        override val defaultPath: BoxPath = BoxPath.Root * "public"

        override fun confirm() { confirmed = true }
    }

    lateinit var presenter: Presenter

    val boxServiceStarter: BoxServiceStarter = Mockito.mock(BoxServiceStarter::class.java)

    val mContext = InstrumentationRegistry.getTargetContext()!!

    val mBoxHelper = UIBoxHelper(mContext)

    @Before
    fun setUp() {
        mBoxHelper.createTokenIfNeeded(false)
        mBoxHelper.removeAllIdentities()
        identity = mBoxHelper.addIdentity("spoon123")
        secondIdentity = mBoxHelper.addIdentity("second")
        identities = listOf(
                EntitySelection(secondIdentity),
                EntitySelection(identity))
    }

    fun launch(identities: List<EntitySelection>? = null): Presenter {
        activityTestRule.launchActivity(defaultIntent)
        presenter = Presenter(identities ?: listOf())
        activity.presenter = presenter
        activity.boxServiceStarter = boxServiceStarter
        wakeLock = UIActionHelper.wakeupDevice(activity)
        mSystemAnimations = SystemAnimations(activity)
        mSystemAnimations?.disableAll()
        return presenter
    }

    @After
    fun cleanUp() {
        wakeLock?.release()
        mSystemAnimations?.enableAll()
    }

    @Test
    fun finishesWithoutIdentities() {
        // the startup sequence doesn't use the mocked presenter
        mBoxHelper.removeAllIdentities()
        launch(listOf())
        assert(activity.isFinishing)
    }

    @Test
    fun choosesAlphabeticalFirstIdentity() {
        launch()
        activity.identity.alias shouldMatch equalTo(identities[0].alias)
    }

    @Test
    fun showDefaultPath() {
        launch()
        Page.folderName.hasText("/Upload")
    }

    @Test
    fun startsFolderChooser() {
        launch()
        Page.startChooser()
        Intents.intended(Page.folderSelectIntentMatcher(activity.identity))
    }

    @Test
    fun reactsToFolderChooserIntent() {
        launch()
        val documentId = DocumentId(identity.keyIdentifier, identity.prefixes.first {
            p ->
            p.type == Prefix.TYPE.USER
        }.prefix, BoxPath.Root / "folder").toString()
        Intents.intending(Page.folderSelectIntentMatcher(
                activity.identity)).respondWith(
                Instrumentation.ActivityResult(
                        Activity.RESULT_OK,
                            Intent().apply {
                                putExtra(FolderChooserActivity.FOLDER_DOCUMENT_ID,
                                        documentId)
                            }))


        Page.startChooser()
        assert(activity.path == BoxPath.Root / "folder")
        Page.folderName.hasText("/folder")
    }

    @Test
    fun loadsFileInfos() {
        launch()
        assert(uri == activity.fileUri)
        assert(activity.filename != "filename")
    }

    @Test
    fun startsUpload() {
        launch()
        assert(!activity.isFinishing)
        val documentId = DocumentId("foo", "bar", BoxPath.Root)
        activity.startUpload(documentId)
        Mockito.verify(boxServiceStarter).startUpload(documentId, uri)
        assert(activity.isFinishing)
    }

    @Test
    fun confirm() {
        launch()
        Page.confirmButton.perform(click())
        assert(presenter.confirmed)
    }

    @Test
    fun selectIdentity() {
        launch()
        Page.selectIdentity(identity)
        assert(activity.identity.alias == identity.alias)
        Page.selectIdentity(secondIdentity)
        assert(activity.identity.alias == secondIdentity.alias)
    }


    fun ViewInteraction.hasText(text: String) {
        check(matches(withText(text)))
    }

    object Page {

        val confirmButton: ViewInteraction
            get() = onView(withId(R.id.identitySelect))

        val identitySpinner: ViewInteraction
            get() = onView(withId(R.id.identitySelect))

        fun selectIdentity(identity: Identity) {
            identitySpinner.perform(click())
            onData(CoreMatchers.equalTo(EntitySelection(identity))).perform(click())
            identitySpinner.check(matches(ViewMatchers.withSpinnerText(
                    Matchers.containsString(identity.alias))))
        }

        val folderButton: ViewInteraction
            get() = onView(withId(R.id.folderSelect))

        val folderName: ViewInteraction
            get() = onView(withId(R.id.folderName))

        fun folderSelectIntentMatcher(identity: EntitySelection) = Matchers.allOf(
                    IntentMatchers.toPackage("de.qabel.qabel.debug"),
                    IntentMatchers.hasExtra(ACTIVE_IDENTITY, identity.keyId))

        fun startChooser() {
            folderButton.perform(click())
        }
    }
}

