package de.qabel.qabelbox.storage.server;

import java.io.File;

import de.qabel.qabelbox.communication.callbacks.DownloadRequestCallback;
import de.qabel.qabelbox.communication.callbacks.JSONModelCallback;
import de.qabel.qabelbox.communication.callbacks.RequestCallback;
import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback;
import de.qabel.qabelbox.storage.model.BoxQuota;

public interface BlockServer {

    String API_QUOTA = "/api/v0/quota/";

    void downloadFile(String prefix, String path, DownloadRequestCallback callback);

    void uploadFile(String prefix, String name, File file, UploadRequestCallback callback);

    void deleteFile(String prefix, String path, RequestCallback callback);

    void getQuota(JSONModelCallback<BoxQuota> callback);
}
