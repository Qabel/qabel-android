package de.qabel.qabelbox.activities;

import android.app.FragmentManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Window;
import android.view.WindowManager;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.ImageViewerFragment;

public class ImageViewerActivity extends CrashReportingActivity {
    public static final String P_URI = "uri";
    public static final String P_TYPE = "type";
    private ImageViewerFragment viewerFragment;
    private final String TAG_IMAGEVIEWER = "TAG_IMAGEVIEWER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageviewer);
        setRotationAnimation();
        initView();
    }

    private void initView() {

        initActionBar();
        initViewerFragment();
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViewerFragment() {
        FragmentManager fm = getFragmentManager();
        viewerFragment = (ImageViewerFragment) fm.findFragmentByTag(TAG_IMAGEVIEWER);

        // If Fragment is not null, then it is currently being retained
        // across a configuration change.
        if (viewerFragment == null) {
            Uri uri = getIntent().getParcelableExtra(P_URI);
            String type = getIntent().getStringExtra(P_TYPE);
            viewerFragment = ImageViewerFragment.newInstance(uri, type);
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, viewerFragment, TAG_IMAGEVIEWER).addToBackStack(null)
                    .commit();
        }
    }

    private void setRotationAnimation() {
        int rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.rotationAnimation = rotationAnimation;
        win.setAttributes(winParams);
    }

    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
