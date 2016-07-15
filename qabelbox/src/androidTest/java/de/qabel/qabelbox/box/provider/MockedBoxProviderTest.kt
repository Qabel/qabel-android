package de.qabel.qabelbox.box.provider

import android.content.Context
import android.test.InstrumentationTestCase
import android.test.mock.MockContentResolver
import de.qabel.qabelbox.BuildConfig
import org.junit.Before

abstract class MockedBoxProviderTest : InstrumentationTestCase() {

    lateinit var provider: MockBoxProvider
    lateinit var mockContentResolver: MockContentResolver

    abstract val context: Context

    @Before
    public override fun setUp() {
        initMockContext()
    }

    private fun initMockContext() {
        provider = MockBoxProvider()
        provider.bindToContext(instrumentation.targetContext)
        mockContentResolver = MockContentResolver()
        mockContentResolver.addProvider(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY,
                provider)
    }
}
