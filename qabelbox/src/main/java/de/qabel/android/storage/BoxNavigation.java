package de.qabel.android.storage;

import android.support.annotation.Nullable;

import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.android.exceptions.QblStorageException;

import java.io.InputStream;
import java.util.List;

public interface BoxNavigation {

	/**
	 * Bumps the version and uploads the metadata file
	 *
	 * All actions are not guaranteed to be finished before the commit
	 * method returned.
	 * @throws QblStorageException
	 */
	void commit() throws QblStorageException;

	boolean hasParent();
	void navigateToParent() throws QblStorageException;
	void navigate(BoxFolder target) throws QblStorageException;
	BoxNavigation navigate(BoxExternalReference target);

	List<BoxFile> listFiles() throws QblStorageException;
	List<BoxFolder> listFolders() throws QblStorageException;

	BoxObject getExternal(String name) throws QblStorageException;

	List<BoxObject> listExternalNames() throws QblStorageException;

	List<BoxObject> listExternals() throws QblStorageException;

	BoxFile upload(String name, InputStream content, @Nullable TransferManager.BoxTransferListener boxTransferListener) throws QblStorageException;
	InputStream download(BoxFile file, @Nullable TransferManager.BoxTransferListener boxTransferListener) throws QblStorageException;

	BoxExternalReference createFileMetadata(QblECPublicKey owner, BoxFile boxFile) throws QblStorageException;
	boolean updateFileMetadata(BoxFile boxFile);
	boolean removeFileMetadata(BoxFile boxFile);

	void attachExternal(BoxExternalReference boxExternalReference) throws QblStorageException;
	void detachExternal(String name) throws QblStorageException;

	void delete(BoxObject boxObject) throws QblStorageException;
	void delete(BoxFile boxFile) throws QblStorageException;
	void delete(BoxFolder boxFolder) throws QblStorageException;
	void delete(BoxExternalReference boxExternal) throws QblStorageException;

	BoxFolder createFolder(String name) throws QblStorageException;

	BoxFile rename(BoxFile file, String name) throws QblStorageException;
	BoxFolder rename(BoxFolder folder, String name) throws QblStorageException;
	BoxExternalReference rename(BoxExternalReference external, String name) throws QblStorageException;

	void reload() throws QblStorageException;

	String getPath();

	String getPath(BoxObject object);
}
