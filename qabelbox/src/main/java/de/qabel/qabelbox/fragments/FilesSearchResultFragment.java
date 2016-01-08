package de.qabel.qabelbox.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.BoxExternal;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;

/**
 * Created by danny on 08.01.2016.
 */
public class FilesSearchResultFragment extends FilesFragment {
    private final String TAG = this.getClass().getSimpleName();

    private static Executor serialExecutor = Executors.newSingleThreadExecutor();

    public static FilesSearchResultFragment newInstance(List<BoxObject> boxVolume) throws QblStorageException {
        FilesSearchResultFragment filesFragment = new FilesSearchResultFragment();
  /*      for (int i = 0; i < boxVolume.size(); i++)
            filesFragment.mBoxVolume = boxVolume.get(i);
*/
        FilesAdapter filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());
        filesFragment.setAdapter(filesAdapter);

        //  filesFragment.setBoxNavigation(boxVolume.navigate());

        filesFragment.fillAdapter();


        return filesFragment;
    }

    @Override
    public String getTitle() {
        return getString(R.string.headline_searchresult);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        final AppCompatActivity act = (AppCompatActivity) getActivity();
        final ActionBar action = act.getSupportActionBar();
        action.setDisplayHomeAsUpEnabled(true);
        self = this;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.ab_main, menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                getActivity().getFragmentManager().popBackStack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    void fillAdapter() {
        filesAdapter.clear();
        /*
        try {
            for (BoxFolder boxFolder : boxNavigation.listFolders()) {
                Log.d(TAG, "Adding folder: " + boxFolder.name);
                filesAdapter.add(boxFolder);
            }
            for (BoxExternal boxExternal : boxNavigation.listExternals()) {
                Log.d("MainActivity", "Adding external: " + boxExternal.name);
                filesAdapter.add(boxExternal);
            }
            for (BoxFile boxFile : boxNavigation.listFiles()) {
                Log.d(TAG, "Adding file: " + boxFile.name);
                filesAdapter.add(boxFile);
            }
        } catch (QblStorageException e) {
            Log.e(TAG, "browseTo failed", e);
        }*/
        filesAdapter.sort();
    }
}
