package de.qabel.qabelbox.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.BlockServer;
import de.qabel.qabelbox.communication.VolumeFileTransferHelper;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.Sanity;
import de.qabel.qabelbox.services.LocalQabelService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by danny on 11.01.2016.
 */
public class SplashActivity extends CrashReportingActivity {

    private final long SPLASH_TIME = 1500;
    private SplashActivity mActivity;
    private AppPreference prefs;
    final private String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mActivity = this;

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_splashscreen);
        setupAppPreferences();
        if(1==2)
        new Thread() {
            public void run() {

                BlockServer bs = new BlockServer();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                LocalQabelService service = QabelBoxApplication.getInstance().getService();
                String prefix = VolumeFileTransferHelper.getPrefixFromIdentity(service.getActiveIdentity());
                String path = "1232";
                byte[] data = new String("Mein File").getBytes();
                CountDownLatch waiter = new CountDownLatch(1);

                upload(bs, prefix, path, data, waiter);
                try {
                    waiter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("finish");
            }

            protected void delete(BlockServer bs, String prefix, String path) {

                bs.deleteFile(mActivity, prefix, path, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        System.out.println(response.toString());
                    }
                });
            }

            protected void download(final BlockServer bs, final String prefix, final String path) {

                bs.downloadFile(mActivity, prefix, path, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        System.out.println(response.toString());
                        System.out.println(response.body().string());
                        delete(bs, prefix, path);
                    }
                });
            }

            protected void upload(final BlockServer bs, final String prefix, final String path, byte[] data, final CountDownLatch waiter) {

                bs.uploadFile(mActivity, prefix, path, data, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        waiter.countDown();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        System.out.println(response.toString());
                        download(bs, prefix, path);
                        waiter.countDown();
                    }
                });
            }
        }.start();
    }

    private void setupAppPreferences() {

        prefs = new AppPreference(this);
        int lastAppStartVersion = prefs.getLastAppStartVersion();

        int currentAppVersionCode = 0;
        try {
            currentAppVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (lastAppStartVersion == 0) {
            prefs.setLastAppStartVersion(currentAppVersionCode);
            //@todo show welcome
        } else {
            if (lastAppStartVersion != currentAppVersionCode) {
                prefs.setLastAppStartVersion(currentAppVersionCode);
                //@todo show whatsnew screen
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startDelayedHandler();
        }
    }

    private void startDelayedHandler() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                startMainActivity();
            }

            private void startMainActivity() {

                if (!Sanity.startWizardActivities(mActivity)) {

                    Intent intent = new Intent(mActivity, MainActivity.class);
                    intent.setAction("");
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            }
        }
                , SPLASH_TIME);
    }
}
