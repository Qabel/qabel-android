package de.qabel.qabelbox.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.ExternalApps;

/**
 * Created by danny on 02.02.16.
 */
public class ImageViewerFragment extends Fragment {

    private Uri uri;
    private String type;
    private Drawable image;

    public static ImageViewerFragment newInstance(final Uri uri, String type) {
        ImageViewerFragment fragment = new ImageViewerFragment();
        fragment.uri = uri;
        fragment.type = type;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.ab_imageviewer, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_imageviewer_edit:
                ExternalApps.openExternApp(getActivity(), uri, type, Intent.ACTION_EDIT);
                return true;
            case R.id.action_imageviewer_open:
                ExternalApps.openExternApp(getActivity(), uri, type, Intent.ACTION_VIEW);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imageviewer, container, false);

        final ImageView iv = (ImageView) view.findViewById(R.id.image);
        final View progressView = view.findViewById(R.id.pb_loading);
        loadImage(iv, progressView);
        setClickListener(view);
        return view;
    }

    private void loadImage(final ImageView iv, final View progressView) {
        if (image != null) {
            iv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            progressView.setVisibility(View.GONE);
            iv.setImageDrawable(image);
        } else {
            Picasso.with(getActivity())
                    .load(uri)
                    .error(R.drawable.image_loading_error)
                    .into(iv, new Callback() {
                        @Override
                        public void onSuccess() {
                            iv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                            progressView.setVisibility(View.GONE);
                            image = iv.getDrawable();
                        }

                        @Override
                        public void onError() {
                            progressView.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void setClickListener(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }
}
