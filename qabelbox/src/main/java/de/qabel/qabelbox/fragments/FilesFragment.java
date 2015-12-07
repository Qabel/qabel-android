package de.qabel.qabelbox.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.BoxExternal;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;


public class FilesFragment extends Fragment {

    private static final String TAG = "FilesFragment";
    protected BoxNavigation boxNavigation;
    private RecyclerView filesListRecyclerView;
    private FilesAdapter filesAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private boolean isLoading;
    private FilesListListener mListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FilesFragment self;
    private static Executor serialExecutor = Executors.newSingleThreadExecutor();

    public static FilesFragment newInstance(final BoxVolume boxVolume) {
        final FilesFragment filesFragment = new FilesFragment();
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
                    Log.e(TAG, "Cannot navigate to root");
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

    /**
     * Sets visibility of loading spinner. Visibility is stored if method is invoked
     * before onCreateView() has completed.
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

    private void setBoxNavigation(BoxNavigation boxNavigation) {
        this.boxNavigation = boxNavigation;
    }

    public BoxNavigation getBoxNavigation() {
        return boxNavigation;
    }

    public interface FilesListListener {
        void onScrolledToBottom(boolean scrolledToBottom);
        void onExport(BoxNavigation boxNavigation, BoxObject object);
        void onDoRefresh(FilesFragment filesFragment, BoxNavigation boxNavigation, FilesAdapter filesAdapter);
    }

    public boolean browseToParent() {
        FutureTask<Boolean> futureHasParent =
                new FutureTask<>(new Callable<Boolean>() {
                    public Boolean call() {
                        return boxNavigation.hasParent();
                    }
                });
        serialExecutor.execute(futureHasParent);

        try {
            if (!futureHasParent.get()) {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "hasParent() failed", e);
            return false;
        }

        new AsyncTask<Void, Void, Void>() {
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
                    Log.e(TAG, "browseTo failed", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                setIsLoading(false);
                filesAdapter.notifyDataSetChanged();
            }
        }.executeOnExecutor(serialExecutor);
        return true;
    }

    private void preBrowseTo() {
        setIsLoading(true);
    }

    private void fillAdapter() {
        filesAdapter.clear();
        try {
            for (BoxFolder boxFolder : boxNavigation.listFolders()){
                Log.d(TAG, "Adding folder: " + boxFolder.name);
                filesAdapter.add(boxFolder);
            }
            for (BoxExternal boxExternal : boxNavigation.listExternals()){
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
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void browseTo(final BoxFolder navigateTo) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                preBrowseTo();
            }
            @Override
            protected Void doInBackground(Void... voids) {
                waitForBoxNavigation();
                try {
                    if (navigateTo != null) {
                        boxNavigation.navigate(navigateTo);
                    }
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
            }
        }.executeOnExecutor(serialExecutor);
    }

}
