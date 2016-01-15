package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
    private String mSearchText;
    private FileSearchFilterFragment.FilterData mFilterData = new FileSearchFilterFragment.FilterData();

    public static FilesSearchResultFragment newInstance(StorageSearch storageSearch, String searchText) {
        FilesSearchResultFragment fragment = new FilesSearchResultFragment();
        FilesAdapter filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());
        fragment.setAdapter(filesAdapter);
        fragment.mSearchResult = storageSearch.filterOnlyFiles();
        fragment.mSearchText = searchText;
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

        if (mSearchResult.getResults().size() <= 2) {
            menu.removeItem(R.id.action_ok);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_ok) {
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
        FileSearchFilterFragment fragment = FileSearchFilterFragment.newInstance(mFilterData, mSearchResult, new FileSearchFilterFragment.CallbackListener() {
            @Override
            public void onSuccess(FileSearchFilterFragment.FilterData data) {
                filterData(data);
            }
        });
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        getFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }


    private void filterData(FileSearchFilterFragment.FilterData data) {
        this.mFilterData = data;

        StorageSearch result = null;
        try {
            result = mSearchResult.clone().filterByName(mSearchText);
            if (data.mDateMin != null) {
                result.filterByMinimumDate(data.mDateMin);
            }
            if (data.mDateMax != null) {
                result.filterByMaximumDate(data.mDateMax);
            }

            result.filterByMinimumSize(data.mFileSizeMin);
            result.filterByMaximumSize(data.mFileSizeMax);
            fillAdapter(result.getResults());
            filesAdapter.notifyDataSetChanged();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "error on clone SearchResult ", e);
        }
    }

    @Override
    public boolean isFabNeeded() {
        return false;
    }

    @Override
    public boolean supportBackButton() {
        return true;
    }
}
