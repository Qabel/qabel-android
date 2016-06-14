package de.qabel.qabelbox.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.navigation.MainNavigator;
import de.qabel.qabelbox.storage.StorageSearch;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxFolder;
import de.qabel.qabelbox.storage.model.BoxObject;
import de.qabel.qabelbox.storage.navigation.BoxNavigation;

public class FilesSearchResultFragment extends FilesFragmentBase {

    public static final String TAG = FilesFragment.class.getSimpleName();
    private StorageSearch mSearchResult;
    private String mSearchText;
    private FileSearchFilterFragment.FilterData mFilterData = new FileSearchFilterFragment.FilterData();
    private AsyncTask<String, Void, StorageSearch> searchTask;

    public static FilesSearchResultFragment newInstance(StorageSearch storageSearch, String searchText) {
        FilesSearchResultFragment fragment = new FilesSearchResultFragment();
        FilesAdapter filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());
        fragment.setAdapter(filesAdapter);
        fragment.mSearchResult = storageSearch.filterOnlyFiles();
        fragment.mSearchText = searchText;
        fragment.fillAdapter(fragment.mSearchResult.filterByName(searchText).getResults());

        fragment.notifyFilesAdapterChanged();
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        setActionBarBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilesSearchResultFragment.this.updateSearchCache();
            }
        });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                FilesSearchResultFragment.this.restartSearch();
            }
        });
        setClickListener();
    }

    @Override
    public void refresh() {
        restartSearch();
    }


    @Override
    public void updateSubtitle() {
        if (actionBar != null) {
            String path = mSearchResult.getPath();
            if(path != null){
                if(path.length() == 1){
                    path = null;
                }else if(path.contains(BoxFolder.RECEIVED_SHARE_NAME)) {
                    path = path.replace(BoxFolder.RECEIVED_SHARE_NAME, getString(R.string.shared_with_you));
                }
            }
            actionBar.setSubtitle(path);
        }
    }

    public FilesAdapter getFilesAdapter() {
        return filesAdapter;
    }

    /**
     * update search cache in files fragment
     */
    private void updateSearchCache() {
        FilesFragment fragment = (FilesFragment) mActivity.getFragmentManager().findFragmentByTag(MainNavigator.TAG_FILES_FRAGMENT);
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
        inflater.inflate(R.menu.ab_files_search_result, menu);
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

    private void showSearchSpinner(boolean visibility) {
        setIsLoading(visibility);
    }

    /**
     * start search
     */
    private void restartSearch() {
        searchTask = new AsyncTask<String, Void, StorageSearch>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showSearchSpinner(true);
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
                    filterData(mFilterData);
                }
            }

            @Override
            protected StorageSearch doInBackground(String... params) {
                try {
                    BoxNavigation nav = ((FilesFragment) getFragmentManager().
                            findFragmentByTag(MainNavigator.TAG_FILES_FRAGMENT)).getBoxNavigation();
                    nav.reload();
                    return new StorageSearch(nav);
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
        return getString(R.string.headline_searchresult) + " \"" + mSearchText + "\"";
    }


    private void handleFilterAction() {
        FileSearchFilterFragment fragment =
                FileSearchFilterFragment.newInstance(mFilterData, mSearchResult, new FileSearchFilterFragment.CallbackListener() {
                    @Override
                    public void onSuccess(FileSearchFilterFragment.FilterData data) {
                        FilesSearchResultFragment.this.filterData(data);
                    }
                });
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        getFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }

    private void filterData(FileSearchFilterFragment.FilterData data) {

        this.mFilterData = data;

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
            notifyFilesAdapterChanged();
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
