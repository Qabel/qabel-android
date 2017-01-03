package de.qabel.qabelbox.chat.view.views

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.R
import de.qabel.qabelbox.adapter.EntitySelectionAdapter
import de.qabel.qabelbox.base.CrashReportingActivity
import de.qabel.qabelbox.contacts.dto.EntitySelection
import de.qabel.qabelbox.chat.dagger.TextShareReceiverModule
import de.qabel.qabelbox.chat.view.presenters.TextShareReceiverPresenter
import de.qabel.qabelbox.dagger.modules.ActivityModule
import kotlinx.android.synthetic.main.content_text_share_receiver.*
import org.jetbrains.anko.ctx
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class TextShareReceiverActivity : TextShareReceiver, CrashReportingActivity(), QabelLog {

    override var identity: EntitySelection? = null
    override var contact: EntitySelection? = null
    override var text: String by Delegates.observable("") {
        kProperty: KProperty<*>, old: String, new: String ->
        if (old != new) {
            receivedText.setText(new, TextView.BufferType.EDITABLE)
        }
    }

    @Inject
    lateinit var presenter: TextShareReceiverPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applicationComponent.plus(ActivityModule(this))
                .plus(TextShareReceiverModule((this)))
                .inject(this)

        setContentView(R.layout.activity_text_share_receiver)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (presenter.availableIdentities.isEmpty()) {
            finish()
            return
        }

        val contactAdapter = EntitySelectionAdapter(ctx, presenter.contacts.toMutableList())
        contactSelect.adapter = contactAdapter
        contactSelect.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                contact = contactAdapter.getItem(position)
            }
        }
        val identityAdapter = EntitySelectionAdapter(ctx, presenter.availableIdentities)
        identitySelect.adapter = identityAdapter
        identitySelect.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                identity = identityAdapter.getItem(position)
                contactAdapter.clear()
                contactAdapter.addAll(presenter.contacts)

            }
        }
        if (Intent.ACTION_SEND == intent.action) {
            text = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.ab_text_receive, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sendMessage) {
            presenter.confirm()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun stop() {
        finish()
    }

    companion object {
        const val TEST_RUN = "TEST_RUN"
    }
}

