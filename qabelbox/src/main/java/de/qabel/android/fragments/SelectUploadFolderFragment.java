package de.qabel.android.fragments;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

import de.qabel.core.config.Identity;
import de.qabel.android.R;
import de.qabel.android.adapter.FilesAdapter;
import de.qabel.android.communication.VolumeFileTransferHelper;
import de.qabel.android.exceptions.QblStorageException;
import de.qabel.android.storage.BoxFolder;
import de.qabel.android.storage.BoxObject;
import de.qabel.android.storage.BoxVolume;

public class SelectUploadFolderFragment extends FilesFragment {

    private final String TAG = this.getClass().getSimpleName();
    private ArrayList<Uri> uris;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;

    private void loadIdentityFiles(final BoxVolume boxVolume) {

        mBoxVolume = boxVolume;
        filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());

        setAdapter(filesAdapter);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                setIsLoading(true);
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    setBoxNavigation(boxVolume.navigate());
                } catch (QblStorageException e) {
                    Log.w(TAG, "Cannot navigate to root. maybe first initialization", e);
                    try {
                        boxVolume.createIndex();
                        setBoxNavigation(boxVolume.navigate());
                    } catch (QblStorageException e1) {
                        Log.e(TAG, "Creating a volume failed", e1);
                        cancel(true);
                        return null;
                    }
                }
                fillAdapter(filesAdapter);
                setClickListener(filesAdapter);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                super.onPostExecute(aVoid);
                setIsLoading(false);

                filesAdapter.notifyDataSetChanged();
            }
        }.executeOnExecutor(serialExecutor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mActivity.fab.hide();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void setClickListener(final FilesAdapter filesAdapter) {

        filesAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
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
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        actionBar.setDisplayHomeAsUpEnabled(true);
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        setActionBarBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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

    public void setAdapter(FilesAdapter adapter) {

        filesAdapter = adapter;
        if (filesListRecyclerView != null) {
            filesListRecyclerView.setAdapter(filesAdapter);
            filesAdapter.notifyDataSetChanged();
        }
    }

    public static SelectUploadFolderFragment newInstance(BoxVolume boxVolume, ArrayList<Uri> data, Identity activeIdentity) {

        SelectUploadFolderFragment fragment = new SelectUploadFolderFragment();
        fillFragmentData(boxVolume, fragment);
        fragment.uris = data;
        fragment.loadIdentityFiles(fragment.getBoxVolume());
        return fragment;
    }

    @Override
    public String getTitle() {

        return getString(R.string.headline_select_upload_folder);
    }

    @Override
    public boolean isFabNeeded() {

        return false;
    }

    public boolean supportBackButton() {

        return true;
    }
}
