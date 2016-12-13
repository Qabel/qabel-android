package de.qabel.qabelbox.startup.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.qabel.core.repository.exception.EntityExistsException
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.R
import de.qabel.qabelbox.identity.interactor.IdentityInteractor
import de.qabel.qabelbox.startup.activities.CreateIdentityActivity
import kotlinx.android.synthetic.main.fragment_create_identity_main.view.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import javax.inject.Inject

class CreateIdentityMainFragment : BaseIdentityFragment() {

    @Inject
    internal lateinit var identityInteractor: IdentityInteractor

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        QabelBoxApplication.getApplicationComponent(activity.applicationContext).inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_create_identity_main, container, false)
        view.bt_create_identity.setOnClickListener {
            mActivity.handleNextClick()
        }
        view.bt_import_identity.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, CreateIdentityActivity.REQUEST_CODE_IMPORT_IDENTITY)
        }
        return view
    }

    override fun check(): String? {
        return null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {
        if (requestCode == CreateIdentityActivity.REQUEST_CODE_IMPORT_IDENTITY && resultCode == Activity.RESULT_OK) {
            importIdentity(mActivity, resultData)
        }
    }

    fun importIdentity(activity: Activity, resultData: Intent?) {
        resultData?.let {
            val uri = resultData.data
            try {
                activity.contentResolver.openFileDescriptor(uri, "r")!!.use {
                    identityInteractor.importIdentity(it.fileDescriptor).subscribe({
                        toast(getString(R.string.idenity_imported))
                        (activity as CreateIdentityActivity).apply {
                            createdIdentity = it
                            completeWizard()
                        }
                    }, {
                        showImportError(it)
                    })
                }
            } catch (e: Throwable) {
                showImportError(e)
            }
        }
    }

    private fun showImportError(ex: Throwable) {
        longToast(when (ex) {
            is EntityExistsException ->
                R.string.create_identity_already_exists
            else ->
                R.string.cant_read_identity
        })
    }
}


