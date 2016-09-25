package de.qabel.qabelbox.index.interactor

import com.natpryce.hamkrest.isEmpty
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.index.IndexService
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.matches
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)

class MainIndexSearchUseCaseTest {

    val service: IndexService = mock()
    val useCase = MainIndexSearchUseCase(service, mock())

    val phone = "+ 49 199 12345678"
    val email = "test@example.com"

    @Test
    fun search() {
        stub(service.searchContacts(email, phone)).toReturn(listOf())

        useCase.search(email, phone) matches isEmpty

        verify(service).searchContacts(email, phone)
    }

}
