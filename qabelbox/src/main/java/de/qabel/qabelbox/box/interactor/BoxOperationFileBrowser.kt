package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxNavigation
import de.qabel.box.storage.ProgressListener
import de.qabel.box.storage.dto.BoxPath
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.core.repository.ContactRepository
import de.qabel.qabelbox.box.BoxScheduler
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.dto.UploadSource
import rx.Observable
import rx.lang.kotlin.observable
import java.io.OutputStream
import javax.inject.Inject

class BoxOperationFileBrowser @Inject constructor(keyAndPrefix: BoxReadFileBrowser.KeyAndPrefix,
                                                  volumeNavigator: VolumeNavigator,
                                                  contactRepo: ContactRepository,
                                                  scheduler: BoxScheduler) :
        BoxReadFileBrowser(keyAndPrefix, volumeNavigator, contactRepo, scheduler), OperationFileBrowser {

    override fun upload(path: BoxPath.File, source: UploadSource): Pair<FileOperationState, Observable<FileOperationState>> {
        val boxFile = source.entry
        val operation = FileOperationState(keyAndPrefix, boxFile.name, path.parent).apply {
            size = boxFile.size
        }
        return Pair(operation, observable<FileOperationState> {
            try {
                it.onNext(operation)
                recursiveCreateFolder(path.parent).upload(path.name, source.source, boxFile.size,
                        object : ProgressListener() {
                            override fun setSize(size: Long) {
                                operation.size = size
                                if (operation.loadDone) {
                                    operation.status = FileOperationState.Status.COMPLETING
                                } else {
                                    operation.status = FileOperationState.Status.LOADING
                                }
                                it.onNext(operation)
                            }

                            override fun setProgress(progress: Long) {
                                operation.done = progress
                                it.onNext(operation)
                            }
                        })
                operation.status = FileOperationState.Status.COMPLETE
                it.onCompleted()
            } catch (e: Throwable) {
                operation.status = FileOperationState.Status.ERROR
                it.onError(e)
            }
        }.subscribeOn(scheduler.rxScheduler))
    }

    override fun download(path: BoxPath.File, targetStream: OutputStream): Pair<FileOperationState, Observable<FileOperationState>> {
        val operation = FileOperationState(keyAndPrefix, path.name, path.parent)
        return Pair(operation, observable<FileOperationState> { subscriber ->
            try {
                subscriber.onNext(operation)
                volumeNavigator.navigateTo(path.parent).apply {
                    val boxFile = getFile(path.name)
                    operation.size = boxFile.size

                    subscriber.onNext(operation)

                    download(boxFile, object : ProgressListener() {

                        override fun setProgress(progress: Long) {
                            operation.done = progress
                            if (operation.loadDone) {
                                operation.status = FileOperationState.Status.COMPLETING
                            } else {
                                operation.status = FileOperationState.Status.LOADING
                            }
                            subscriber.onNext(operation)
                        }

                        override fun setSize(size: Long) {
                            operation.size = size
                            subscriber.onNext(operation)
                        }

                    }).use {
                        it.copyTo(targetStream)
                    }
                }
                operation.status = FileOperationState.Status.COMPLETE
                subscriber.onCompleted()
            } catch(ex: Throwable) {
                operation.status = FileOperationState.Status.ERROR
                subscriber.onError(ex)
            }
        }.doOnUnsubscribe { operation.status = FileOperationState.Status.CANCELED }
                .subscribeOn(scheduler.rxScheduler))
    }

    override fun delete(path: BoxPath): Observable<Unit> = observable<Unit> {
        subscriber ->
        try {
            subscriber.onNext(Unit)
            val nav = volumeNavigator.navigateTo(path.parent)
            when (path) {
                is BoxPath.Folder -> nav.getFolder(path.name).let { nav.delete(it) }
                is BoxPath.File -> nav.getFile(path.name).let { nav.delete(it) }
            }
            subscriber.onNext(Unit)
        } catch (e: QblStorageNotFound) {
        } catch (e: QblStorageException) {
            subscriber.onError(e)
            return@observable
        }
        subscriber.onCompleted()
    }.subscribeOn(scheduler.rxScheduler)

    override fun createFolder(path: BoxPath.FolderLike): Observable<Unit> =
            observable<Unit> { subscriber ->
                try {
                    subscriber.onNext(Unit)
                    recursiveCreateFolder(path)
                    subscriber.onNext(Unit)
                    subscriber.onCompleted()
                } catch (e: Throwable) {
                    subscriber.onError(e)
                }
            }.subscribeOn(scheduler.rxScheduler)

    private fun recursiveCreateFolder(path: BoxPath.FolderLike): BoxNavigation =
            volumeNavigator.navigateTo(path) { p, nav ->
                nav.listFolders().find { it.name == p.name } ?: nav.createFolder(p.name)
                nav.commitIfChanged()
            }

}
