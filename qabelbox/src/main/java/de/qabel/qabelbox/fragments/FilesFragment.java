package de.qabel.qabelbox.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;


public class FilesFragment extends Fragment {

    protected BoxNavigation boxNavigation;
    private RecyclerView filesListRecyclerView;
    private FilesAdapter filesAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private ProgressBar loadingSpinner;
    private boolean showLoadingSpinner;


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
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_files, container, false);

        loadingSpinner = (ProgressBar) view.findViewById(R.id.loadingSpinner);
        if (showLoadingSpinner) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }
        else {
            loadingSpinner.setVisibility(View.INVISIBLE);
        }

        filesListRecyclerView = (RecyclerView) view.findViewById(R.id.files_list);
        filesListRecyclerView.setHasFixedSize(true);

        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        filesListRecyclerView.setLayoutManager(recyclerViewLayoutManager);

        filesListRecyclerView.setAdapter(filesAdapter);
        registerForContextMenu(filesListRecyclerView);

        return view;
    }

    /**
     * Sets visibility of loading spinner. Visibility is stored if method is invoked
     * before onCreateView() has completed.
     * @param isVisible
     */
    public void setLoadingSpinner(boolean isVisible) {
        showLoadingSpinner = isVisible;
        if (loadingSpinner == null) {
            return;
        }
        if (isVisible) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }
        else {
            loadingSpinner.setVisibility(View.INVISIBLE);
        }
    }

    public void setAdapter(FilesAdapter adapter) {
        filesAdapter = adapter;
    }

    //TODO: Workaround for navigation
    public void setBoxNavigation(BoxNavigation boxNavigation) {
        this.boxNavigation = boxNavigation;
    }
}
