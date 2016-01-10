package de.qabel.qabelbox.fragments;

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

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.StorageSearch;

/**
 * Created by danny on 08.01.2016.
 */
public class FilesSearchResultFragment extends FilesFragment {
    protected static final String TAG = "FilesSearchResFragment";
    private StorageSearch mSearchResult;

    public static FilesSearchResultFragment newInstance(StorageSearch storageSearch, String searchText) {
        FilesSearchResultFragment fragment = new FilesSearchResultFragment();
        FilesAdapter filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());
        fragment.setAdapter(filesAdapter);
        fragment.mSearchResult = storageSearch.filterOnlyFiles();
        fragment.fillAdapter(fragment.mSearchResult.filterByName(searchText).getResults());
        filesAdapter.notifyDataSetChanged();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();
        mActivity.fab.hide();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ab_files_search_result, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_filter) {
            handleFilterAction();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * set list click listener
     */
    private void setClickListener() {
        setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final BoxObject boxObject = getFilesAdapter().get(position);
                if (boxObject instanceof BoxFile) {
                    mActivity.showFile(boxObject);
                }
            }

            @Override
            public void onItemLockClick(View view, int position) {
            }
        });
    }

    /**
     * fill adapter with file list
     *
     * @param results
     */
    private void fillAdapter(List<BoxObject> results) {
        filesAdapter.clear();
        for (BoxObject boxObject : results) {
            filesAdapter.add(boxObject);
        }
        filesAdapter.sort();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setClickListener();
        return v;
    }

    @Override
    public String getTitle() {
        return getString(R.string.headline_searchresult);
    }

    private void handleFilterAction() {
        Toast.makeText(getActivity(), "tbd: Filter action.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean supportBackButton() {
        return true;
    }
}
