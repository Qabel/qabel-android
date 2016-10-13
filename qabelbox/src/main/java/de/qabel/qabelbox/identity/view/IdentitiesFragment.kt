package de.qabel.qabelbox.identity.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cocosw.bottomsheet.BottomSheet
import de.qabel.core.config.ContactExportImport
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.config.IdentityExportImport
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.helper.UIHelper
import de.qabel.qabelbox.identity.interactor.IdentityUseCase
import de.qabel.qabelbox.identity.view.adapter.IdentitiesAdapter
import de.qabel.qabelbox.navigation.Navigator
import kotlinx.android.synthetic.main.fragment_identities.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * TODO extract functionality, create presenter
 */
class IdentitiesFragment : BaseFragment(showFAButton = true) {

    private val identityListAdapter: IdentitiesAdapter = IdentitiesAdapter({
        navigator.selectIdentityDetails(it)
    }, {
        showBottomSheet(it); true
    })

    private var identityToExport: Identity? = null

    @Inject
    internal lateinit var navigator: Navigator
    @Inject
    internal lateinit var identityUseCase: IdentityUseCase

    override val title: String
        get() = getString(R.string.headline_identities)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(MainActivityComponent::class.java)
        component.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_identities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        identity_list.setHasFixedSize(true)
        identity_list.layoutManager = LinearLayoutManager(view.context)
        identity_list.adapter = identityListAdapter
    }

    private fun showBottomSheet(identity: Identity) {
        BottomSheet.Builder(activity).title(identity.alias).sheet(R.menu.bottom_sheet_identities)
                .listener { dialog, which ->
                    when (which) {
                        R.id.identity_edit -> navigator.selectIdentityDetails(identity)
                        R.id.identities_delete -> {
                            alert({
                                title(R.string.confirm_delete_identity_header)
                                message(String.format(getString(R.string.confirm_delete_identity_message), identity.alias))
                                positiveButton(R.string.ok, {
                                    identityUseCase.deleteIdentity(identity).subscribe({
                                        reload()
                                        runOnUiThread {
                                            toast(getString(R.string.entry_deleted).format(identity.alias))
                                        }
                                    }, {
                                        showDefaultError(it)
                                    })
                                })
                            }).show()
                        }
                        R.id.identities_export -> {
                            exportIdentity(identity)
                        }
                        R.id.identities_export_as_contact -> {
                            exportIdentityAsContact(identity)
                        }
                        R.id.identities_export_as_contact_qrcode -> {
                            navigator.selectQrCodeFragment(identity.toContact())
                        }
                    }
                }.show()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        identityUseCase.getIdentities().subscribe({
            identityListAdapter.init(it.identities.toList())
        })
    }

    private fun exportIdentity(identity: Identity) {
        startExportFileChooser(identity, QabelSchema.FILE_PREFIX_IDENTITY, QabelSchema.FILE_SUFFIX_IDENTITY, MainActivity.REQUEST_EXPORT_IDENTITY)
    }

    private fun exportIdentityAsContact(identity: Identity) {
        startExportFileChooser(identity, QabelSchema.FILE_PREFIX_CONTACT, QabelSchema.FILE_SUFFIX_CONTACT, MainActivity.REQUEST_EXPORT_IDENTITY_AS_CONTACT)
    }

    private fun startExportFileChooser(identity: Identity, type: String, filesuffix: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, type + "" + identity.alias + "." + filesuffix)
        identityToExport = identity
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MainActivity.REQUEST_EXPORT_IDENTITY || requestCode == MainActivity.REQUEST_EXPORT_IDENTITY_AS_CONTACT) {
                val exportIdentity = identityToExport
                if (resultData != null && exportIdentity != null) {
                    val uri = resultData.data
                    try {
                        activity.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                            FileOutputStream(pfd.fileDescriptor).use { fileOutputStream ->
                                if (requestCode == MainActivity.REQUEST_EXPORT_IDENTITY_AS_CONTACT) {
                                    fileOutputStream.write(ContactExportImport.exportIdentityAsContact(exportIdentity).toByteArray())
                                    UIHelper.showDialogMessage(activity, R.string.dialog_headline_info, R.string.identity_as_contact_export_successfully)
                                } else {
                                    fileOutputStream.write(IdentityExportImport.exportIdentity(exportIdentity).toByteArray())
                                    UIHelper.showDialogMessage(activity, R.string.dialog_headline_info, R.string.identity_export_successfully)
                                }
                            }
                        } ?: throw FileNotFoundException()
                    } catch (e: IOException) {
                        UIHelper.showDialogMessage(activity, R.string.dialog_headline_info, R.string.identity_export_failed, e)
                    }
                }
            }
        }
    }

}
