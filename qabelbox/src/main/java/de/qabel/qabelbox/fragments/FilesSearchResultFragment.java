package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.StorageSearch;

/**
 * Created by danny on 08.01.2016.
 */
public class FilesSearchResultFragment extends FilesFragment {
    protected static final String TAG = "FilesSearchResFragment";
    protected StorageSearch mSearchResult;
    private MainActivity mMainActivity;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get item selected and deal with it
        System.out.println("frag "+item.getItemId()+" "+android.R.id.home);
        switch (item.getItemId()) {
            case android.R.id.home:
                //called when the up affordance/carat in actionbar is pressed
                mActivity.onBackPressed();
                return true;
        }
        return false;
    }
    public static FilesSearchResultFragment newInstance(StorageSearch storageSearch, String searchText) throws QblStorageException {
        FilesSearchResultFragment filesFragment = new FilesSearchResultFragment();
        FilesAdapter filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());
        filesFragment.setAdapter(filesAdapter);
        filesFragment.mSearchResult = storageSearch.filterOnlyFiles();
        filesFragment.fillAdapter(filesFragment.mSearchResult.filterByName(searchText).getResults());
        filesAdapter.notifyDataSetChanged();

        return filesFragment;
    }

    private void setClickListener() {
        setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final BoxObject boxObject = getFilesAdapter().get(position);
                if (boxObject instanceof BoxFile) {
                    mMainActivity.showFile(boxObject);
                }
            }

            @Override
            public void onItemLockClick(View view, int position) {

            }


        });
    }


    void fillAdapter(List<BoxObject> results) {
        filesAdapter.clear();
        for (BoxObject boxObject : results) {
            filesAdapter.add(boxObject);
        }
        filesAdapter.sort();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMainActivity = (MainActivity) activity;
        setClickListener();
    }

    @Override
    public String getTitle() {
        return getString(R.string.headline_searchresult);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //showUpButton();
        action.setDisplayHomeAsUpEnabled(true);

       action.setDefaultDisplayHomeAsUpEnabled(true);

        self = this;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ab_files_search_result, menu);
    }



    private void handleFilterAction() {
        Toast.makeText(getActivity(),"tbd: Filter action.", Toast.LENGTH_SHORT).show();
    }

}
