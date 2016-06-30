package de.qabel.qabelbox.ui.adapters


import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.dto.EntryType
import de.qabel.qabelbox.dto.FileEntry
import de.qabel.qabelbox.helper.FontHelper
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class,
        shadows = arrayOf(TextViewFontShadow::class), manifest = "src/main/AndroidManifest.xml")
class FileAdapterTest {

    lateinit var adapter: FileAdapter

    val sample = FileEntry("Foo.txt", Date(), 42, EntryType.FILE)

    @Before
    fun setUp() {
        adapter = FileAdapter(mutableListOf())
        FontHelper.disable = true;
    }

    @Test
    fun testGetItemCount() {
        adapter.itemCount shouldMatch  equalTo(0)
        adapter.files.add(sample)
        adapter.itemCount shouldMatch  equalTo(1)
    }

    @Test
    fun itemAt() {
        val second = sample.copy(name = "Bar.txt")
        adapter.files = mutableListOf(sample, second)
        adapter.getItemAtPosition(0)!! shouldMatch equalTo(sample)
        adapter.getItemAtPosition(1)!! shouldMatch equalTo(second)
    }

}

