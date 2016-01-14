package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;

public class SelectUploadFolderFragment extends FilesFragment {

    private RecyclerView filesListRecyclerView;
    private FilesAdapter filesAdapter;
    private Uri uri;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private OnSelectedUploadFolderListener mListener;
    private Button buttonUpload;
    private Button buttonAbortUpload;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_upload_folder, container, false);

        filesListRecyclerView = (RecyclerView) view.findViewById(R.id.files_list);
        filesListRecyclerView.setHasFixedSize(true);

        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        filesListRecyclerView.setLayoutManager(recyclerViewLayoutManager);

        filesListRecyclerView.setAdapter(filesAdapter);

        buttonUpload = (Button) view.findViewById(R.id.buttonUpload);
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onFolderSelected(uri, boxNavigation);
            }
        });

        buttonAbortUpload = (Button) view.findViewById(R.id.buttonAbortUpload);
        buttonAbortUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onAbort();
            }
        });

        return view;
    }

    public void setAdapter(FilesAdapter adapter) {
        filesAdapter = adapter;
        if (filesListRecyclerView != null) {
            filesListRecyclerView.setAdapter(filesAdapter);
            filesAdapter.notifyDataSetChanged();
        }
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSelectedUploadFolderListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectedUploadFolderListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnSelectedUploadFolderListener {
        void onFolderSelected(Uri uri, BoxNavigation boxNavigation);
        void onAbort();
    }

    @Override
    public boolean isFabNeeded() {
        return false;
    }

    public boolean supportBackButton() {
        return false;
    }
}
