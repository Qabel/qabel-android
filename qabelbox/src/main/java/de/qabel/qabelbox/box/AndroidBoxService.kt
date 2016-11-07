package de.qabel.qabelbox.box

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import de.qabel.core.event.EventSink
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.events.FileDownloadEvent
import de.qabel.qabelbox.box.events.FileUploadEvent
import de.qabel.qabelbox.box.interactor.DocumentIdInteractor
import de.qabel.qabelbox.box.notifications.StorageNotificationManager
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.toDocumentId
import de.qabel.qabelbox.reporter.CrashSubmitter
import org.jetbrains.anko.ctx
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import rx.Observable
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AndroidBoxService : Service(), QabelLog {

    @Inject
    lateinit var useCase: DocumentIdInteractor
    @Inject
    lateinit var notificationManager: StorageNotificationManager
    @Inject
    lateinit var eventSink: EventSink

    @Inject
    lateinit var crashSubmitter: CrashSubmitter

    private val pendingMap: MutableMap<DocumentId, Observable<FileOperationState>> = mutableMapOf()

    override fun onCreate() {
        super.onCreate()
        QabelBoxApplication.getApplicationComponent(applicationContext).inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            handleIntent(intent, startId)
        }
        return START_NOT_STICKY
    }

    private fun handleIntent(intent: Intent, startId: Int) {
        try {
            debug("received intent $intent $startId ${intent.action}")
            when (intent.action) {
                Actions.UPLOAD_FILE -> {
                    val documentId = intent.getStringExtra(KEY_DOC_ID).toDocumentId()
                    val uri: Uri = intent.getParcelableExtra(KEY_SOURCE_URI)
                    uploadFile(documentId, uri, startId)
                }
                Actions.DOWNLOAD_FILE -> {
                    val documentId = intent.getStringExtra(KEY_DOC_ID).toDocumentId()
                    val targetFile: File = intent.getSerializableExtra(KEY_TARGET_FILE) as File
                    downloadFile(documentId, targetFile, startId)
                }
            }
        } catch (ex: Throwable) {
            error("Error handling file intent", ex)
            crashSubmitter.submit(ex)
        }
    }

    private fun isPendingOperation(documentId: DocumentId) = pendingMap.containsKey(documentId)

    /**
     * Remove pending operation, stop service if all operations done.
     */
    private fun handleOperationComplete(documentId: DocumentId, startId: Int) {
        synchronized(pendingMap, {
            pendingMap.remove(documentId)
            if (pendingMap.isEmpty()) {
                debug("Operations done. Stop service.")
                this@AndroidBoxService.stopSelf(startId)
            }
        })
    }

    private fun uploadFile(documentId: DocumentId, uri: Uri, startId: Int) {
        if (isPendingOperation(documentId)) {
            debug("DocumentId is in progress $documentId")
            ctx.runOnUiThread {
                longToast(ctx.getString(R.string.message_file_cant_upload))
            }
            return
        }
        debug("Starting upload $uri to $documentId")
        useCase.uploadFile(uri, documentId).let {
            val (operation, observable) = it
            observable.doOnCompleted {
                notificationManager.updateUploadNotification(operation)
                ctx.runOnUiThread {
                    longToast(ctx.getString(R.string.upload_complete_notification_msg, operation.entryName))
                }
                eventSink.push(FileUploadEvent(operation))
                handleOperationComplete(documentId, startId)
            }.sample(100L, TimeUnit.MILLISECONDS).subscribe({
                notificationManager.updateUploadNotification(it)
                eventSink.push(FileUploadEvent(it))
            }, {
                error("Error uploading File $uri to ${documentId.path}", it)
                operation.completed = true
                eventSink.push(FileUploadEvent(operation))
                handleOperationComplete(documentId, startId)
            }).let {
                pendingMap.put(documentId, observable)
            }
        }
    }

    private fun downloadFile(documentId: DocumentId, targetFile: File, startId: Int) {
        if (isPendingOperation(documentId)) {
            debug("DocumentId is in progress $documentId")
            ctx.runOnUiThread {
                longToast(ctx.getString(R.string.hockeyapp_download_failed_dialog_title))
            }
            return
        }
        debug("Starting download $documentId to $targetFile")
        useCase.downloadFile(documentId, targetFile).let {
            val (operation, observable) = it
            observable.doOnCompleted {
                //TODO notificationManager
                ctx.runOnUiThread {
                    longToast(ctx.getString(R.string.upload_complete_notification_msg, operation.entryName))
                }
                eventSink.push(FileDownloadEvent(operation))
                handleOperationComplete(documentId, startId)
            }.sample(100L, TimeUnit.MILLISECONDS).subscribe({
                //TODO notificationManager
                eventSink.push(FileDownloadEvent(it))
            }, {
                error("Error downloading File ${documentId.path} to $targetFile", it)
                operation.completed = true
                eventSink.push(FileDownloadEvent(operation))
                handleOperationComplete(documentId, startId)
            }).let {
                pendingMap.put(documentId, observable)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        //TODO can be nice to bind the service to activities and track progress
        return null
    }

    object Actions {
        const val UPLOAD_FILE = "upload_file"
        const val DOWNLOAD_FILE = "download_file"
    }

    companion object {
        const val KEY_DOC_ID = "doc_id"
        const val KEY_SOURCE_URI = "sourceUri"
        const val KEY_TARGET_FILE = "targetFile"
    }

}

object BoxServiceInteractor {
    fun startUpload(context: Context, documentId: DocumentId, source: Uri) {
        context.startService(Intent(AndroidBoxService.Actions.UPLOAD_FILE, null,
                context, AndroidBoxService::class.java).apply {
            putExtra(AndroidBoxService.KEY_DOC_ID, documentId.toString())
            putExtra(AndroidBoxService.KEY_SOURCE_URI, source)
        })
    }
}
