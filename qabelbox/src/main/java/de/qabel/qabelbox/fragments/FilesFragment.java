package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.BoxExternal;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.storage.StorageSearch;

public class FilesFragment extends BaseFragment {

    private static final String TAG = "FilesFragment";
    protected BoxNavigation boxNavigation;
    private RecyclerView filesListRecyclerView;
    protected FilesAdapter filesAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private boolean isLoading;
    private FilesListListener mListener;
    protected SwipeRefreshLayout swipeRefreshLayout;
    private FilesFragment self;
    private AsyncTask<Void, Void, Void> browseToTask;

    private MenuItem mSearchAction;
    private boolean isSearchOpened = false;
    private EditText edtSeach;
    private BoxVolume mBoxVolume;
    private AsyncTask<String, Void, StorageSearch> searchTask;
    private StorageSearch mCachedStorageSearch;

    public static FilesFragment newInstance(final BoxVolume boxVolume) {

        final FilesFragment filesFragment = new FilesFragment();
        filesFragment.mBoxVolume = boxVolume;
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
                    Log.e(TAG, "Cannot navigate to root", e);
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
        return filesFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        final AppCompatActivity act = (AppCompatActivity) getActivity();
        final ActionBar action = act.getSupportActionBar();
        action.setTitle(getTitle());
        self = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_files, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                mListener.onDoRefresh(self, boxNavigation, filesAdapter);
            }
        });

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {

                swipeRefreshLayout.setRefreshing(isLoading);
            }
        });
        filesListRecyclerView = (RecyclerView) view.findViewById(R.id.files_list);
        filesListRecyclerView.setHasFixedSize(true);

        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        filesListRecyclerView.setLayoutManager(recyclerViewLayoutManager);

        filesListRecyclerView.setAdapter(filesAdapter);

        filesListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                super.onScrolled(recyclerView, dx, dy);
                int lastCompletelyVisibleItem = ((LinearLayoutManager) recyclerViewLayoutManager).findLastCompletelyVisibleItemPosition();
                int firstCompletelyVisibleItem = ((LinearLayoutManager) recyclerViewLayoutManager).findFirstCompletelyVisibleItemPosition();
                if (lastCompletelyVisibleItem == filesAdapter.getItemCount() - 1
                        && firstCompletelyVisibleItem > 0) {
                    mListener.onScrolledToBottom(true);
                } else {
                    mListener.onScrolledToBottom(false);
                }
            }
        });
        return view;
    }



    /*@Override
    public void onResume() {
        super.onResume();
        setIsLoading(isLoading);
    }*/

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        try {
            mListener = (FilesListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FilesListListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.ab_files, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        mSearchAction = menu.findItem(R.id.action_search);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.action_search:
                if (!isSearchRunning()) {
                    handleMenuSearch();
                } else if (isSearchOpened) {
                    removeSearchInActionbar(actionBar);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isSearchRunning() {

        if (isSearchOpened) {

            return true;
        }
        return searchTask != null && ((!searchTask.isCancelled() && searchTask.getStatus() != AsyncTask.Status.FINISHED));
    }

    /**
     * handle click on search icon
     */
    private void handleMenuSearch() {

        if (isSearchOpened) {
            removeSearchInActionbar(actionBar);
        } else {
            openSearchInActionBar(actionBar);
        }
    }

    /**
     * setup the actionbar to show a input dialog for search keyword
     *
     * @param action
     */
    private void openSearchInActionBar(final ActionBar action) {

        action.setDisplayShowCustomEnabled(true);
        action.setCustomView(R.layout.ab_search_field);
        action.setDisplayShowTitleEnabled(false);

        edtSeach = (EditText) action.getCustomView().findViewById(R.id.edtSearch);

        //add editor action listener
        edtSeach.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String text = edtSeach.getText().toString();
                    removeSearchInActionbar(action);
                    startSearch(text);
                    return true;
                }
                return false;
            }
        });

        edtSeach.requestFocus();

        //open keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(edtSeach, InputMethodManager.SHOW_IMPLICIT);
        mActivity.fab.hide();
        mSearchAction.setIcon(R.drawable.ic_ab_close);
        isSearchOpened = true;
    }

    /**
     * restore the actionbar
     *
     * @param action
     */
    private void removeSearchInActionbar(ActionBar action) {

        action.setDisplayShowCustomEnabled(false);
        action.setDisplayShowTitleEnabled(true);

        //hides the keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edtSeach.getWindowToken(), 0);

        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.RESULT_HIDDEN);
        mSearchAction.setIcon(R.drawable.ic_ab_search);
        action.setTitle(getTitle());
        isSearchOpened = false;
        mActivity.fab.show();
    }

    @Override
    public void onPause() {

        if (isSearchOpened) {
            removeSearchInActionbar(actionBar);
        }
        super.onPause();
    }

    /**
     * start search
     *
     * @param searchText
     */
    private void startSearch(final String searchText) {

        cancelSearchTask();
        searchTask = new AsyncTask<String, Void, StorageSearch>() {

            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                setIsLoading(true);
            }

            @Override
            protected void onCancelled(StorageSearch storageSearch) {

                setIsLoading(false);
                super.onCancelled(storageSearch);
            }

            @Override
            protected void onPostExecute(StorageSearch storageSearch) {

                setIsLoading(false);

                //check if files found
                if (storageSearch == null || storageSearch.filterOnlyFiles().getResults().size() == 0) {
                    Toast.makeText(getActivity(), R.string.no_entrys_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mActivity.isFinishing() && !searchTask.isCancelled()) {
                    boolean needRefresh = mCachedStorageSearch != null;
                    try {
                        mCachedStorageSearch = storageSearch.clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }

                    FilesSearchResultFragment fragment = FilesSearchResultFragment.newInstance(mCachedStorageSearch, searchText, needRefresh);
                    mActivity.toggle.setDrawerIndicatorEnabled(false);
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, FilesSearchResultFragment.TAG).addToBackStack(null).commit();
                }
            }

            @Override
            protected StorageSearch doInBackground(String... params) {

                try {
                    if (mCachedStorageSearch != null && mCachedStorageSearch.getResults().size() > 0) {
                        return mCachedStorageSearch;
                    }

                    return new StorageSearch(mBoxVolume.navigate());
                } catch (QblStorageException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
        searchTask.executeOnExecutor(serialExecutor);
    }

    private void cancelSearchTask() {

        if (searchTask != null) {
            searchTask.cancel(true);
        }
    }

    /**
     * Sets visibility of loading spinner. Visibility is stored if method is invoked
     * before onCreateView() has completed.
     *
     * @param isLoading
     */
    public void setIsLoading(final boolean isLoading) {

        this.isLoading = isLoading;
        if (swipeRefreshLayout == null) {
            return;
        }
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {

                swipeRefreshLayout.setRefreshing(isLoading);
            }
        });
    }

    public void setAdapter(FilesAdapter adapter) {

        filesAdapter = adapter;
    }

    public FilesAdapter getFilesAdapter() {

        return filesAdapter;
    }

    public void setOnItemClickListener(FilesAdapter.OnItemClickListener onItemClickListener) {

        filesAdapter.setOnItemClickListener(onItemClickListener);
    }

    @Override
    public boolean isFabNeeded() {

        return true;
    }

    private void setBoxNavigation(BoxNavigation boxNavigation) {

        this.boxNavigation = boxNavigation;
    }

    public BoxNavigation getBoxNavigation() {

        return boxNavigation;
    }

    @Override
    public String getTitle() {

        return getString(R.string.headline_files);
    }

    /**
     * handle back pressed
     *
     * @return true if back handled
     */
    public boolean handleBackPressed() {

        if (isSearchOpened) {
            removeSearchInActionbar(actionBar);
            return true;
        }
        if (searchTask != null && ((!searchTask.isCancelled() && searchTask.getStatus() != AsyncTask.Status.FINISHED))) {
            cancelSearchTask();
            return true;
        }

        return false;
    }

    public BoxVolume getBoxVolume() {

        return mBoxVolume;
    }

    public void setCachedSearchResult(StorageSearch searchResult) {

        mCachedStorageSearch = searchResult;
    }

    public interface FilesListListener {

        void onScrolledToBottom(boolean scrolledToBottom);

        void onExport(BoxNavigation boxNavigation, BoxObject object);

        void onDoRefresh(FilesFragment filesFragment, BoxNavigation boxNavigation, FilesAdapter filesAdapter);
    }

    public boolean browseToParent() {

        cancelBrowseToTask();

        if (!boxNavigation.hasParent()) {
            return false;
        }

        browseToTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                preBrowseTo();
            }

            @Override
            protected Void doInBackground(Void... voids) {

                waitForBoxNavigation();
                try {
                    boxNavigation.navigateToParent();
                    fillAdapter();
                } catch (QblStorageException e) {
                    Log.d(TAG, "browseTo failed", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                super.onPostExecute(aVoid);
                setIsLoading(false);
                filesAdapter.notifyDataSetChanged();
            }
        };
        browseToTask.executeOnExecutor(serialExecutor);
        return true;
    }

    private void preBrowseTo() {

        setIsLoading(true);
    }

    private void fillAdapter() {

        filesAdapter.clear();

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
        }
        filesAdapter.sort();
    }

    private void waitForBoxNavigation() {

        while (boxNavigation == null) {
            Log.d(TAG, "waiting for BoxNavigation");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void browseTo(final BoxFolder navigateTo) {

        Log.d(TAG, "Browsing to " + navigateTo.name);
        cancelBrowseToTask();
        browseToTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                preBrowseTo();
            }

            @Override
            protected Void doInBackground(Void... voids) {

                waitForBoxNavigation();
                try {
                    boxNavigation.navigate(navigateTo);
                    fillAdapter();
                } catch (QblStorageException e) {
                    Log.e(TAG, "browseTo failed", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                super.onPostExecute(aVoid);
                setIsLoading(false);
                filesAdapter.notifyDataSetChanged();
                browseToTask = null;
            }

            @Override
            protected void onCancelled() {

                super.onCancelled();
                setIsLoading(false);
                browseToTask = null;
                Toast.makeText(getActivity(), R.string.aborted,
                        Toast.LENGTH_SHORT).show();
            }
        };
        browseToTask.executeOnExecutor(serialExecutor);
    }

    private void cancelBrowseToTask() {

        if (browseToTask != null) {
            Log.d(TAG, "Found a running browseToTask");
            browseToTask.cancel(true);
            Log.d(TAG, "Canceled browserToTask");
        }
    }
}
