package de.qabel.qabelbox.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.storage.BoxObject;


public class FilesFragment extends Fragment {

    protected BoxNavigation boxNavigation;
    private RecyclerView filesListRecyclerView;
    private FilesAdapter filesAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private boolean isLoading;
    private FilesListListener mListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FilesFragment self;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.file_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                Snackbar.make(filesListRecyclerView, "Pos " + filesAdapter.getLongClickedPosition(), Snackbar.LENGTH_SHORT).show();
                return true;
            case R.id.delete:
                return true;
            case R.id.export:
                // Export handled in the MainActivity
                BoxObject boxObject = filesAdapter.get(filesAdapter.getLongClickedPosition());
                if (boxObject instanceof BoxFolder) {
                    Toast.makeText(getActivity(), R.string.folder_export_not_implemented,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                mListener.onExport(boxNavigation, boxObject);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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
        registerForContextMenu(filesListRecyclerView);

        filesListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastCompletelyVisibleItem = ((LinearLayoutManager) recyclerViewLayoutManager).findLastCompletelyVisibleItemPosition();
                int firstCompletelyVisibleItem = ((LinearLayoutManager) recyclerViewLayoutManager).findFirstCompletelyVisibleItemPosition();
                if (lastCompletelyVisibleItem == filesAdapter.getItemCount() - 1
                        && firstCompletelyVisibleItem > 0 ) {
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

    //TODO: Workaround for navigation
    public void setBoxNavigation(BoxNavigation boxNavigation) {
        this.boxNavigation = boxNavigation;
    }

    public interface FilesListListener {
        void onScrolledToBottom(boolean scrolledToBottom);
        void onExport(BoxNavigation boxNavigation, BoxObject object);
        void onDoRefresh(FilesFragment filesFragment, BoxNavigation boxNavigation, FilesAdapter filesAdapter);
    }
}
