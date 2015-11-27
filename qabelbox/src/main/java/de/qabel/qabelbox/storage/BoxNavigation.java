package de.qabel.qabelbox.storage;

import android.support.annotation.Nullable;

import de.qabel.qabelbox.exceptions.QblStorageException;

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

	BoxNavigation navigate(BoxFolder target) throws QblStorageException;
	BoxNavigation navigate(BoxExternal target);

	List<BoxFile> listFiles() throws QblStorageException;
	List<BoxFolder> listFolders() throws QblStorageException;
	List<BoxExternal> listExternals() throws QblStorageException;

	BoxFile upload(String name, InputStream content, @Nullable TransferManager.BoxTransferListener boxTransferListener) throws QblStorageException;
	InputStream download(BoxFile file, @Nullable TransferManager.BoxTransferListener boxTransferListener) throws QblStorageException;

	BoxFolder createFolder(String name) throws QblStorageException;

	void delete(BoxFile file) throws QblStorageException;
	void delete(BoxFolder folder) throws QblStorageException;
	void delete(BoxExternal external) throws QblStorageException;

	BoxFile rename(BoxFile file, String name) throws QblStorageException;
	BoxFolder rename(BoxFolder folder, String name) throws QblStorageException;
	BoxExternal rename(BoxExternal external, String name) throws QblStorageException;

}
