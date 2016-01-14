package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 14.01.2016.
 */
public class FileSearchFilterFragment extends BaseFragment {
    private FilterData mFilterData;
    private CallbackListener mListener;

    public static FileSearchFilterFragment newInstance(FilterData data, CallbackListener listener) {
        FileSearchFilterFragment fragment = new FileSearchFilterFragment();
        fragment.mFilterData = data;
        fragment.mListener = listener;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_files_search_filter, container, false);
        return view;

    }

    @Override
    public String getTitle() {
        return getString(R.string.headline_filefilter);
    }

    @Override
    public boolean supportBackButton() {
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ab_files_search_filter, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_ok) {
            getFragmentManager().popBackStack();
            mListener.onSuccess(mFilterData);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class FilterData {
        Date mDateMin;
        Date mDateMax;
        int mfileSizeMin = Integer.MIN_VALUE;
        int mFileSizeMax = Integer.MAX_VALUE;
    }

    public interface CallbackListener {
        void onSuccess(FilterData data);
    }
}
