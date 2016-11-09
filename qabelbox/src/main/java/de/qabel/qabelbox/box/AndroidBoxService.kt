package de.qabel.qabelbox.box

import android.app.Service
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
import rx.Subscription
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

    private val pendingMap: MutableMap<DocumentId, Pair<Observable<FileOperationState>, Subscription>> = mutableMapOf()

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
                    uploadFile(documentId, intent.data, startId)
                }
                Actions.DOWNLOAD_FILE -> {
                    val documentId = intent.getStringExtra(KEY_DOC_ID).toDocumentId()
                    downloadFile(documentId, intent.data, startId)
                }
                Actions.CANCEL_OPERATION -> {
                    val documentId = intent.getStringExtra(KEY_DOC_ID)
                    //TODO Define schema
                    val (observable, subscription) = pendingMap.keys.find {
                        it.path.toString() == documentId
                    }.let { pendingMap[it]!! }

                    if (!subscription.isUnsubscribed) {
                        debug("Cancelling operation for $documentId")
                        ctx.runOnUiThread {
                            longToast("Cancel $documentId")
                        }
                        subscription.unsubscribe()
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
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
                ctx.runOnUiThread {
                    longToast(ctx.getString(R.string.upload_complete_msg, operation.entryName))
                }
                eventSink.push(FileUploadEvent(operation))
                notificationManager.updateUploadNotification(operation)
                handleOperationComplete(documentId, startId)
            }.sample(150L, TimeUnit.MILLISECONDS).subscribe({
                notificationManager.updateUploadNotification(it)
                eventSink.push(FileUploadEvent(it))
            }, {
                error("Error uploading File $uri to ${documentId.path}", it)
                eventSink.push(FileUploadEvent(operation))
                handleOperationComplete(documentId, startId)
            }).let {
                pendingMap.put(documentId, Pair(observable, it))
            }
        }
    }

    private fun downloadFile(documentId: DocumentId, targetUri: Uri, startId: Int) {
        if (isPendingOperation(documentId)) {
            debug("DocumentId is in progress $documentId")
            ctx.runOnUiThread {
                //TODO Add own label
                longToast(ctx.getString(R.string.hockeyapp_download_failed_dialog_title))
            }
            return
        }
        debug("Starting download $documentId to $targetUri")
        useCase.downloadFile(documentId, targetUri).let {
            val (operation, observable) = it
            observable.doOnCompleted {
                ctx.runOnUiThread {
                    longToast(ctx.getString(R.string.upload_complete_msg, operation.entryName))
                }
                notifyForDownload(operation)
                handleOperationComplete(documentId, startId)
            }.sample(250L, TimeUnit.MILLISECONDS)
                    .doOnUnsubscribe {
                        //TODO Called onComplete too
                        debug("Download unsubscribed ${documentId.path} to $targetUri", it)
                        operation.status = FileOperationState.Status.CANCELED
                        notifyForDownload(operation)
                        handleOperationComplete(documentId, startId)
                    }
                    .subscribe({ notifyForDownload(it) }, {
                        if (it is BoxOperationInterrupt) {
                            debug("Download canceled ${documentId.path} to $targetUri", it)
                            operation.status = FileOperationState.Status.CANCELED
                        } else {
                            error("Error downloading File ${documentId.path} to $targetUri", it)
                            operation.status = FileOperationState.Status.ERROR
                        }
                        notifyForDownload(operation)
                        handleOperationComplete(documentId, startId)
                    })
                    .let {
                        pendingMap.put(documentId, Pair(observable, it))
                    }
        }
    }

    private fun notifyForDownload(operation: FileOperationState) {
        notificationManager.updateDownloadNotification(operation)
        eventSink.push(FileDownloadEvent(operation))
    }

    override fun onBind(intent: Intent?): IBinder? {
        //TODO can be nice to bind the service to activities and track progress
        return null
    }

    object Actions {
        const val UPLOAD_FILE = "upload_file"
        const val DOWNLOAD_FILE = "download_file"
        const val CANCEL_OPERATION = "cancel_operation"
    }

    companion object {
        const val KEY_DOC_ID = "doc_id"
    }

}
