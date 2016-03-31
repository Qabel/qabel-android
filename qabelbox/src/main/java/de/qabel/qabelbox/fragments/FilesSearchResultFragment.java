package de.qabel.qabelbox.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.menu;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.adapter.FilesAdapter.OnItemClickListener;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.fragments.FileSearchFilterFragment.CallbackListener;
import de.qabel.qabelbox.fragments.FileSearchFilterFragment.FilterData;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.StorageSearch;

import java.util.ArrayList;
import java.util.List;

public class FilesSearchResultFragment extends FilesFragment {

    protected static final String TAG = FilesFragment.class.getSimpleName();
    private StorageSearch mSearchResult;
    private String mSearchText;
    private FilterData mFilterData = new FilterData();
    private AsyncTask<String, Void, StorageSearch> searchTask;
    private boolean mNeedRefresh;
    private MenuItem mFilterItem;

    public static FilesSearchResultFragment newInstance(StorageSearch storageSearch, String searchText, boolean needRefresh) {

        FilesSearchResultFragment fragment = new FilesSearchResultFragment();
        FilesAdapter filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());
        fragment.setAdapter(filesAdapter);
        fragment.mSearchResult = storageSearch.filterOnlyFiles();
        fragment.mSearchText = searchText;
        fragment.mNeedRefresh = needRefresh;
        fragment.fillAdapter(fragment.mSearchResult.filterByName(searchText).getResults());

        filesAdapter.notifyDataSetChanged();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                updateSearchCache();
            }
        });

        mActivity.fab.hide();
    }

    /**
     * update search cache in files fragment
     */
    private void updateSearchCache() {

        FilesFragment fragment = (FilesFragment) mActivity.getFragmentManager().findFragmentByTag(MainActivity.TAG_FILES_FRAGMENT);
        if (fragment != null) {
            fragment.setCachedSearchResult(mSearchResult);
        }
    }

    @Override
    public void onBackPressed() {

        updateSearchCache();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(menu.ab_files_search_result, menu);
        mFilterItem = menu.findItem(id.action_ok);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == id.action_ok) {
            handleFilterAction();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * set list click listener
     */
    private void setClickListener() {

        setOnItemClickListener(new OnItemClickListener() {
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
        swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {

                restartSearch();
            }
        });
        setClickListener();
        if (!mNeedRefresh) {
            showSearchSpinner(false);
        } else {
            showSearchSpinner(true);
        }
        return v;
    }

    private void showSearchSpinner(boolean visibility) {

        setIsLoading(visibility);
    }

    @Override
    public void onResume() {

        super.onResume();
        if (mNeedRefresh) {
            restartSearch();
        }
    }

    /**
     * start search
     */
    private void restartSearch() {
        //
        showSearchSpinner(true);

        searchTask = new AsyncTask<String, Void, StorageSearch>() {

            @Override
            protected void onPreExecute() {

                super.onPreExecute();
            }

            @Override
            protected void onCancelled(StorageSearch storageSearch) {

                super.onCancelled(storageSearch);
                showSearchSpinner(false);
            }

            @Override
            protected void onPostExecute(StorageSearch storageSearch) {

                if (!mActivity.isFinishing() && !searchTask.isCancelled()) {

                    showSearchSpinner(false);
                    mSearchResult = storageSearch;
                    mNeedRefresh = false;
                    filterData(mFilterData);
                }
            }

            @Override
            protected StorageSearch doInBackground(String... params) {

                try {
                    return new StorageSearch(((FilesFragment) getFragmentManager().findFragmentByTag(MainActivity.TAG_FILES_FRAGMENT)).getBoxVolume().navigate());
                } catch (QblStorageException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
        searchTask.executeOnExecutor(serialExecutor);
    }

    @Override
    public String getTitle() {

        return getString(string.headline_searchresult);
    }

    private void handleFilterAction() {

        FileSearchFilterFragment fragment = FileSearchFilterFragment.newInstance(mFilterData, mSearchResult, new CallbackListener() {
            @Override
            public void onSuccess(FilterData data) {

                filterData(data);
            }
        });
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        getFragmentManager().beginTransaction().add(id.fragment_container, fragment).addToBackStack(null).commit();
    }

    private void filterData(FilterData data) {

        mFilterData = data;

        StorageSearch result;
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
