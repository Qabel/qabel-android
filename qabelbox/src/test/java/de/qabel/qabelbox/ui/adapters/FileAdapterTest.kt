package de.qabel.qabelbox.ui.adapters


import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.notNull
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.dto.BrowserEntry
import de.qabel.qabelbox.helper.FontHelper
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import kotlinx.android.synthetic.main.item_contacts.view.*
import kotlinx.android.synthetic.main.item_files.view.*
import org.jetbrains.anko.imageResource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class,
        shadows = arrayOf(TextViewFontShadow::class), manifest = "src/main/AndroidManifest.xml")
class FileAdapterTest {

    var adapter = FileAdapter(mutableListOf())

    val sampleFile = BrowserEntry.File("Foo.txt", 42, Date(0))
    val sampleFolder = BrowserEntry.Folder("FooFolder")

    val activity = Robolectric.buildActivity(Activity::class.java).create().get();

    val view: View by lazy {
        LayoutInflater.from(activity).inflate(R.layout.item_files, null)
    }

    @Before
    fun setUp() {
        FontHelper.disable = true
    }

    @Test
    fun testGetItemCount() {
        adapter.itemCount shouldMatch  equalTo(0)
        adapter.entries.add(sampleFile)
        adapter.itemCount shouldMatch  equalTo(1)
    }

    @Test
    fun itemAt() {
        adapter.entries = mutableListOf(sampleFile, sampleFolder)
        adapter.getItemAtPosition(0)!!.name shouldMatch equalTo(sampleFile.name)
        adapter.getItemAtPosition(1)!!.name shouldMatch equalTo(sampleFolder.name)
    }

    @Test
    fun onBindViewHolder() {
        val holder = spy(FileViewHolder(view))
        adapter.entries.add(sampleFile)
        adapter.onBindViewHolder(holder, 0)
        verify(holder).bindTo(sampleFile)
    }

    @Test
    fun bindViewHolderToFile() {
        FileViewHolder(view).bindTo(sampleFile)
        view.entryName reads sampleFile.name
        checkNotNull(view.fileEntryIcon.drawable)
        view.modificationTime reads "Jan 1, 1970"
    }

    @Test
    fun bindViewHolderToFolder() {
        FileViewHolder(view).bindTo(sampleFolder)
        view.entryName reads sampleFolder.name
        checkNotNull(view.fileEntryIcon.drawable)
        view.modificationTime reads ""
    }

    infix fun TextView.reads(expected: String) {
        this.text.toString() shouldMatch equalTo(expected)
    }


    fun bindViewHolder() {
        adapter.bindViewHolder(FileViewHolder(view), 0)
    }

    private fun equalsSampleFile(clickedOn: BrowserEntry?) {
        val entry = clickedOn ?: throw AssertionError("entry is null")
        entry.name shouldMatch equalTo(sampleFile.name)
    }


    @Test
    fun clickHandler() {
        var clickedOn: BrowserEntry? = null
        adapter = FileAdapter(mutableListOf(sampleFile), click = {clickedOn = it} )
        bindViewHolder()

        view.performClick()

        equalsSampleFile(clickedOn)
    }

    @Test
    fun longClickHandler() {
        var longClickedOn: BrowserEntry? = null
        adapter = FileAdapter(mutableListOf(sampleFile),
                longClick = {
                    longClickedOn = it
                    true
                })
        bindViewHolder()

        view.performLongClick()

        equalsSampleFile(longClickedOn)
    }

}

