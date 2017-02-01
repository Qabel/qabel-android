package de.qabel.qabelbox.box.provider

import android.Manifest
import android.content.Context
import android.content.pm.ProviderInfo
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.TestConstants
import de.qabel.box.storage.dto.BoxPath
import de.qabel.client.box.documentId.DocumentId
import de.qabel.qabelbox.box.interactor.DocumentIdAdapter
import java.io.File

class MockBoxProvider : BoxProvider() {

    val targetFile: File by lazy { createTempFile() }

    override fun createTmpFile(): File = targetFile

    override fun inject() {
    }

    fun injectProvider(documentIdAdapter: DocumentIdAdapter) {
        useCase = documentIdAdapter
    }

    @Throws(Exception::class)
    fun bindToContext(context: Context) {
        val info = ProviderInfo()
        info.authority = BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY
        info.exported = true
        info.grantUriPermissions = true
        info.readPermission = Manifest.permission.MANAGE_DOCUMENTS
        info.writePermission = Manifest.permission.MANAGE_DOCUMENTS
        attachInfo(context, info)
    }

    companion object {
        val prefix = TestConstants.PREFIX
        val PUB_KEY = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a"
        val ROOT = DocumentId(PUB_KEY, prefix, BoxPath.Root)
    }
}

