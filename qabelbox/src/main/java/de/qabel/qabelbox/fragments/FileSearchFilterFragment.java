package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.Formater;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.StorageSearch;

/**
 * Created by danny on 14.01.2016.
 */
public class FileSearchFilterFragment extends BaseFragment {
    private FilterData mFilterData;
    private CallbackListener mListener;
    private TextView mTvMinFileSize;
    private TextView mTvMaxFileSize;
    private TextView mTvMinDate;
    private TextView mTvMaxDate;
    private StorageSearch mSearchResult;
    long mMinFileSize;
    long mMaxFileSize;
    long mMinDate;
    long mMaxDate;

    public static FileSearchFilterFragment newInstance(FilterData data, StorageSearch searchResult, CallbackListener listener) {
        FileSearchFilterFragment fragment = new FileSearchFilterFragment();
        fragment.mFilterData = data;
        fragment.mListener = listener;
        fragment.mSearchResult = searchResult;
        fragment.generateMinMax();
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
        mTvMinFileSize = (TextView) view.findViewById(R.id.tvMinFileSize);
        mTvMaxFileSize = (TextView) view.findViewById(R.id.tvMaxFileSize);
        mTvMinDate = (TextView) view.findViewById(R.id.tvMinDate);
        mTvMaxDate = (TextView) view.findViewById(R.id.tvMaxDate);
        updateInitialUI();
        return view;

    }

    private void updateInitialUI() {
        mTvMinFileSize.setText(Formater.formatFileSizeHumanReadable(mMinFileSize));
        mTvMaxFileSize.setText(Formater.formatFileSizeHumanReadable(mMaxFileSize));
        mTvMaxDate.setText(Formater.formatDateShort(mMaxDate * 1000));
        mTvMinDate.setText(Formater.formatDateShort(mMinDate * 1000));
    }

    private void generateMinMax() {
        mMinFileSize = Integer.MAX_VALUE;
        mMaxFileSize = Integer.MIN_VALUE;
        mMaxDate = 0;
        mMinDate = System.currentTimeMillis() / 1000;

        for (BoxObject item : mSearchResult.getResults()) {
            if (item instanceof BoxFile) {
                BoxFile file = (BoxFile) item;
                mMinFileSize = Math.min(file.size, mMinFileSize);
                mMaxFileSize = Math.max(file.size, mMaxFileSize);
                mMinDate = Math.min(file.mtime, mMinDate);
                mMaxDate = Math.max(file.mtime, mMaxDate);

            }
        }
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
        if (id == R.id.action_use_filter) {
            getFragmentManager().popBackStack();
            mListener.onSuccess(mFilterData);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class FilterData {
        Date mDateMin;
        Date mDateMax;
        int mFileSizeMin = 0;
        int mFileSizeMax = Integer.MAX_VALUE;
    }

    public interface CallbackListener {
        void onSuccess(FilterData data);
    }
}
