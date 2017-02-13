package de.qabel.qabelbox.ui.views

import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withId
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.EntitySelection
import de.qabel.qabelbox.box.views.FolderChooserActivity
import de.qabel.qabelbox.chat.view.presenters.TextShareReceiverPresenter
import de.qabel.qabelbox.chat.view.views.TextShareReceiverActivity
import de.qabel.qabelbox.hasText
import de.qabel.qabelbox.ui.helper.UIBoxHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class TextShareReceiverActivityTest {


    @JvmField
    @Rule
    var activityTestRule: IntentsTestRule<TextShareReceiverActivity> = IntentsTestRule(
            TextShareReceiverActivity::class.java, false, false)
    val activity: TextShareReceiverActivity
        get() = activityTestRule.activity

    lateinit var identity: Identity
    lateinit var secondIdentity: Identity

    lateinit var identities: List<EntitySelection>

    val text = "text"

    val defaultIntent: Intent
        get() {
            return Intent(InstrumentationRegistry.getTargetContext(), FolderChooserActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(TextShareReceiverActivity.TEST_RUN, true)
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }

    open class Presenter(override val availableIdentities: List<EntitySelection>,
                         override val contacts: List<EntitySelection>)
        : TextShareReceiverPresenter {

        var confirmed = false

        override fun confirm() {
            confirmed = true
        }

    }

    lateinit var presenter: Presenter

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
        presenter = Presenter(identities ?: listOf(), listOf())
        activity.presenter = presenter
        return presenter
    }

    @Test
    fun finishesWithoutIdentities() {
        // the startup sequence doesn't use the mocked presenter
        mBoxHelper.removeAllIdentities()
        launch(listOf())
        activity.isFinishing shouldMatch equalTo(true)
    }

    @Test
    fun choosesAlphabeticalFirstIdentity() {
        launch(identities)
        activity.identity!!.alias shouldMatch equalTo(identities[0].alias)
    }

    @Test
    fun showReceivedText() {
        launch(identities)
        Page.textField.hasText(text)
    }

    @Test
    fun sendMessage() {
        launch(identities)
        presenter.confirmed shouldMatch equalTo(false)
        Page.confirmButton.perform(click())
        presenter.confirmed shouldMatch equalTo(true)
    }

    @Test
    fun activityCanFinish() {
        launch()
        activity.isFinishing shouldMatch equalTo(false)
        activity.stop()
        activity.isFinishing shouldMatch equalTo(true)
    }


    object Page {

        val confirmButton: ViewInteraction
            get() = onView(withId(R.id.sendMessage))

        val textField: ViewInteraction
            get() = onView(withId(R.id.receivedText))

    }
}

