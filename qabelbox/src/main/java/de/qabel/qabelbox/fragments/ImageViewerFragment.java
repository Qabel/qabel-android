package de.qabel.qabelbox.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.qabel.qabelbox.R.drawable;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.helper.ExternalApps;

public class ImageViewerFragment extends BaseFragment {
    private Uri uri;
    private String type;

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

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(menu.ab_imageviewer, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case id.action_imageviewer_edit:
                ExternalApps.openExternApp(getActivity(), uri, type, Intent.ACTION_EDIT);
                return true;
            case id.action_imageviewer_open:
                ExternalApps.openExternApp(getActivity(), uri, type, Intent.ACTION_VIEW);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public String getTitle() {
        return getString(string.headline_imageviewer);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(layout.fragment_imageviewer, container, false);

        final ImageView iv = (ImageView) view.findViewById(id.image);
        final View progressView = view.findViewById(id.pb_loading);
        Picasso.with(getActivity())
                .load(uri)
                .error(drawable.image_loading_error)
                .into(iv, new Callback() {
                    @Override
                    public void onSuccess() {
                        iv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                        progressView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError() {
                        progressView.setVisibility(View.GONE);
                    }
                });
        setClickListener(view);
        setActionBarBackListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        return view;
    }

    private void setClickListener(View view) {
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

    }

    @Override
    public boolean supportBackButton() {
        return true;
    }
}
