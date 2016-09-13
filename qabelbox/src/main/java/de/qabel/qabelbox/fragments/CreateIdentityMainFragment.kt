package de.qabel.qabelbox.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import de.qabel.core.repository.exception.EntityExistsException
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.CreateIdentityActivity
import de.qabel.qabelbox.identity.interactor.IdentityUseCase
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import javax.inject.Inject

class CreateIdentityMainFragment : BaseIdentityFragment(), View.OnClickListener {

    private var mCreateIdentity: Button? = null
    private var mImportIdentity: Button? = null

    @Inject
    internal lateinit var identityUseCase: IdentityUseCase

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        QabelBoxApplication.getApplicationComponent(activity.applicationContext).inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_create_identity_main, container, false)
        mCreateIdentity = view.findViewById(R.id.bt_create_identity) as Button
        mImportIdentity = view.findViewById(R.id.bt_import_identity) as Button
        mCreateIdentity!!.setOnClickListener(this)
        mImportIdentity!!.setOnClickListener(this)
        return view
    }

    override fun check(): String? {
        return null
    }

    override fun onClick(v: View) {
        if (v === mCreateIdentity) {
            mActivity.handleNextClick()
        }
        if (v === mImportIdentity) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, CreateIdentityActivity.REQUEST_CODE_IMPORT_IDENTITY)
        }
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
                    identityUseCase.importIdentity(it.fileDescriptor).subscribe({
                        toast(getString(R.string.idenity_imported))
                        (activity as CreateIdentityActivity).apply {
                            createdIdentity = it
                            completeWizard()
                        }
                    }, {
                        throw it
                    })
                }
            } catch (e: Throwable) {
                when (e) {
                    is EntityExistsException ->
                        longToast(R.string.create_identity_already_exists)
                    else ->
                        longToast(R.string.cant_read_identity)
                }
            }
        }
    }
}


