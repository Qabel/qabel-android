package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;

/**
 * Created by danny on 02.02.16.
 */
public class ImageViewerFragment extends BaseFragment {

    private Uri uri;
    private String type;

    public static ImageViewerFragment newInstance(final Uri uri, String type) {

        ImageViewerFragment fragment = new ImageViewerFragment();
        fragment.uri = uri;
        fragment.type = type;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_imageviewer, container, false);

        final ImageView iv = (ImageView) view.findViewById(R.id.image);
        Picasso.with(getActivity())
                .load(uri)
                .error(R.drawable.image_loading_error)
                .placeholder(R.drawable.image_loading_animation)
                .into(iv, new Callback() {
                    @Override
                    public void onSuccess() {

                        iv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                    }

                    @Override
                    public void onError() {

                    }
                });
        setClickListener(view);
        setActionBarBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getActivity().onBackPressed();
            }
        });
        return view;
    }

    private void setClickListener(View view) {

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getActivity().onBackPressed();
            }
        });
        view.findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getActivity().onBackPressed();
                showImage(getActivity(), uri, type, Intent.ACTION_VIEW);
            }
        });
        view.findViewById(R.id.edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getActivity().onBackPressed();
                showImage(getActivity(), uri, type, Intent.ACTION_EDIT);
            }
        });
    }

    public static void showImage(Activity activity, Uri uri, String type, String action) {

        Intent viewIntent = new Intent();
        viewIntent.setDataAndType(uri, type);
        viewIntent.setAction(action);
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(Intent.createChooser(viewIntent, "Open with"), MainActivity.REQUEST_EXTERN_VIEWER_APP);
    }

    @Override
    public boolean supportBackButton() {

        return true;
    }

}
