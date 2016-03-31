package de.qabel.qabelbox.activities;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Window;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.ImageViewerFragment;

/**
 * Created by danny on 31.03.16.
 */
public class ImageViewerActivity extends CrashReportingActivity {
    public static final String P_URI = "uri";
    public static final String P_TYPE = "type";
    private String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_imageviewer);

        Uri uri = getIntent().getParcelableExtra(P_URI);
        String type = getIntent().getStringExtra(P_TYPE);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar ab = getSupportActionBar();

        ab.setDisplayShowHomeEnabled(true);
        ab.setDisplayHomeAsUpEnabled(true);

        ImageViewerFragment viewerFragment = ImageViewerFragment.newInstance(uri, type);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, viewerFragment).addToBackStack(null)
                .commit();


    }

    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
