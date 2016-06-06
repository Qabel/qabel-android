package de.qabel.qabelbox.storage.server;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.callbacks.DownloadRequestCallback;
import de.qabel.qabelbox.communication.callbacks.JSONModelCallback;
import de.qabel.qabelbox.communication.callbacks.RequestCallback;
import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback;
import de.qabel.qabelbox.storage.model.BoxQuota;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MockBlockServer implements BlockServer {

    public static final long SIZE = 2000;
    public static final long QUOTA = 22000;

    @Override
    public void downloadFile(String prefix, String path, DownloadRequestCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uploadFile(String prefix, String name, File file, UploadRequestCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteFile(String prefix, String path, RequestCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getQuota(JSONModelCallback<BoxQuota> callback) {
        try {
            Response response = new Response.Builder()
                    .request(new Request.Builder().url(TestConstants.BLOCK_URL).build())
                    .protocol(Protocol.HTTP_1_0)
                    .code(200)
                    .body(ResponseBody.create(MediaType.parse("application/json"),
                            createMockQuotaResponse())).build();

            callback.onResponse(null, response);
        }catch (IOException e){
            Log.e("MockBlockServer", "Cannot get mock quota");
        }
    }

    private String createMockQuotaResponse(){
        try {
            JSONObject quotaMockData = new JSONObject();
            quotaMockData.put("size", SIZE);
            quotaMockData.put("quota", QUOTA);
            return quotaMockData.toString();
        }catch (JSONException e){
            Log.e("MockBlockServer", "Cannot init data", e);
        }
        return null;
    }

    @Override
    public int getNextId() {
        return new Random().nextInt();
    }
}
