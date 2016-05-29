package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.communication.VolumeFileTransferHelper;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.model.BoxFolder;
import de.qabel.qabelbox.storage.model.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;

public class SelectUploadFolderFragment extends FilesFragment {

    private final String TAG = this.getClass().getSimpleName();

    private ArrayList<Uri> uris;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        actionBar.setDisplayHomeAsUpEnabled(true);
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        mActivity.fab.hide();
    }

    @Override
    public void setAdapter(FilesAdapter adapter) {
        super.setAdapter(adapter);
        setClickListener();
    }

    private void setClickListener() {
        setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                final BoxObject boxObject = filesAdapter.get(position);
                if (boxObject != null) {
                    if (boxObject instanceof BoxFolder) {
                        browseTo(((BoxFolder) boxObject));
                    }
                }
            }

            @Override
            public void onItemLockClick(View view, int position) {

            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ab_upload, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_upload) {
            uploadFiles();
            return true;
        }
        return false;
    }

    private void uploadFiles() {
        for (int i = 0; i < uris.size(); i++) {
            VolumeFileTransferHelper.upload(getActivity(), uris.get(i), boxNavigation, mActivity.boxVolume);
        }
        Toast.makeText(getActivity(), getString(R.string.x_files_uploading).replace("%1", "" + uris.size()), Toast.LENGTH_SHORT).show();
        getFragmentManager().popBackStack();
    }

    public static SelectUploadFolderFragment newInstance(BoxVolume boxVolume, ArrayList<Uri> data, Identity activeIdentity) {
        SelectUploadFolderFragment fragment = new SelectUploadFolderFragment();
        fragment.uris = data;
        //fragment.loadIdentityFiles(boxVolume);
        return fragment;
    }

    @Override
    public String getTitle() {
        return getString(R.string.headline_select_upload_folder);
    }
}
