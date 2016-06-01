package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.listeners.IdleCallback;

public abstract class FilesFragmentBase extends BaseFragment {

    private RecyclerView filesListRecyclerView;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private boolean isLoading;
    private FilesListListener mListener;

    protected SwipeRefreshLayout swipeRefreshLayout;
    protected FilesAdapter filesAdapter;

    protected View mEmptyView;
    protected View mLoadingView;

    private IdleCallback idleCallback;

    public interface FilesListListener {

        void onScrolledToBottom(boolean scrolledToBottom);

        void onDoRefresh(FilesFragmentBase filesFragment);

    }

    public abstract void refresh();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getTitle());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_files, container, false);
        setupLoadingViews(view);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> mListener.onDoRefresh(this));

        swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(isLoading));
        filesListRecyclerView = (RecyclerView) view.findViewById(R.id.files_list);
        filesListRecyclerView.setHasFixedSize(false);

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

    protected void setupLoadingViews(View view) {
        mEmptyView = view.findViewById(R.id.empty_view);
        mLoadingView = view.findViewById(R.id.loading_view);
        final ProgressBar pg = (ProgressBar) view.findViewById(R.id.pb_firstloading);
        pg.setIndeterminate(true);
        pg.setEnabled(true);
        if (filesAdapter != null)
            filesAdapter.setEmptyView(mEmptyView, mLoadingView);
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
     *
     * @param isLoading
     */
    public void setIsLoading(final boolean isLoading) {
        this.isLoading = isLoading;
        runIdleCallback(!isLoading);
        if (swipeRefreshLayout == null) {
            return;
        }
        if (!isLoading) {
            mLoadingView.setVisibility(View.GONE);
        }
        swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(isLoading));
    }

    protected void notifyFilesAdapterChanged() {
        new Handler(Looper.getMainLooper()).
                post(() -> {
                    if (filesListRecyclerView != null && !filesListRecyclerView.isComputingLayout()) {
                        filesAdapter.notifyDataSetChanged();
                    }
                });
    }


    public void setAdapter(FilesAdapter adapter) {
        filesAdapter = adapter;
        filesAdapter.setEmptyView(mEmptyView, mLoadingView);
        if (filesListRecyclerView != null) {
            filesListRecyclerView.setAdapter(filesAdapter);
        }
        notifyFilesAdapterChanged();
    }


    public void injectIdleCallback(IdleCallback callback) {
        idleCallback = callback;
    }

    public void runIdleCallback(boolean isIdle) {
        if (idleCallback == null) {
            return;
        }
        if (isIdle) {
            idleCallback.idle();
        } else {
            idleCallback.busy();
        }
    }

    public FilesAdapter getFilesAdapter() {
        return filesAdapter;
    }

    public void setOnItemClickListener(FilesAdapter.OnItemClickListener onItemClickListener) {
        filesAdapter.setOnItemClickListener(onItemClickListener);
    }

    @Override
    public boolean supportSubtitle() {
        return true;
    }

}
