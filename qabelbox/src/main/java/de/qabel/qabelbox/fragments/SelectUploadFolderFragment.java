package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
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
import java.util.Set;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;

public class SelectUploadFolderFragment extends FilesFragment {

    private final String TAG = this.getClass().getSimpleName();
    private RecyclerView filesListRecyclerView;
    private FilesAdapter filesAdapter;
    private Uri uri;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private OnSelectedUploadFolderListener mListener;

    private void loadIdentityFiles(final BoxVolume boxVolume) {

        final FilesFragment filesFragment = new FilesFragment();
        mBoxVolume = boxVolume;
        final FilesAdapter filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());

        filesFragment.setAdapter(filesAdapter);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                filesFragment.setIsLoading(true);
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    filesFragment.setBoxNavigation(boxVolume.navigate());
                } catch (QblStorageException e) {
                    Log.w(TAG, "Cannot navigate to root. maybe first initialization", e);
                    try {
                        boxVolume.createIndex();
                        filesFragment.setBoxNavigation(boxVolume.navigate());
                    } catch (QblStorageException e1) {
                        Log.e(TAG, "Creating a volume failed", e1);
                        cancel(true);
                        return null;
                    }
                }
                filesFragment.fillAdapter();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                super.onPostExecute(aVoid);
                filesFragment.setIsLoading(false);
                filesAdapter.notifyDataSetChanged();
            }
        }.executeOnExecutor(serialExecutor);

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

        mActivity.fab.hide();

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

        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        final Set<Identity> identities = service.getIdentities().getIdentities();
        if (identities.size() > 1) {
            new SelectUploadIdentityDialog(mActivity, new SelectUploadIdentityDialog.Result() {
                @Override
                public void onCancel() {
                    UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.share_into_app_canceled);
                    onBackPressed();
                }

                @Override
                public void onIdentitySelected(Identity identity) {
                    identitySelected(identity);


                }
            });
        } else {
            identitySelected(service.getActiveIdentity());
        }
    }

    private void identitySelected(Identity identity) {
        QabelBoxApplication.getInstance().getService().setActiveIdentity(identity);
        mActivity.refreshFilesBrowser(identity);
        Toast.makeText(getActivity(), "Start uploading " + identity.getAlias(), Toast.LENGTH_SHORT).show();
        loadIdentityFiles(mActivity.boxVolume);
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
            //getFragmentManager().popBackStack();
        }
        return false;
    }

    private void uploadFiles() {
        mListener.onFolderSelected(uri, boxNavigation);
    }

    public void onFolderSelected(final Uri uri, final BoxNavigation boxNavigation) {
        mActivity.onFolderSelected(uri, boxNavigation);
        /*
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                Cursor returnCursor =
                        mActivity.getContentResolver().query(uri, null, null, null, null);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                String name = returnCursor.getString(nameIndex);
                returnCursor.close();

                try {
                    String path = boxNavigation.getPath();
                    String folderId = getBoxVolume().getDocumentId(path);



                    Uri uploadUri = DocumentsContract.buildDocumentUri(
                            BoxProvider.AUTHORITY, folderId + name);

                    InputStream content = mActivity.getContentResolver().openInputStream(uri);
                    OutputStream upload = mActivity.getContentResolver().openOutputStream(uploadUri, "w");
                    if (upload == null || content == null) {
                        //finish();
                        return null;
                    }
                    IOUtils.copy(content, upload);
                    content.close();
                    upload.close();



                } catch (IOException e) {
                    Log.e(TAG, "Upload failed", e);
                }
                return null;
            }
        }.execute();
        // finish();*/
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
    public String getTitle() {
        return getString(R.string.headline_select_upload_folder);
    }

    @Override
    public boolean isFabNeeded() {
        return false;
    }

    public boolean supportBackButton() {
        return false;
    }
}
