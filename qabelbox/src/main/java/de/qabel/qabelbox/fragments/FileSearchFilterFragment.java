package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
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
public class FileSearchFilterFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener {
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
    private SeekBar mSbFileSizeMin;
    private SeekBar mSbFileSizeMax;

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
        mSbFileSizeMin = (SeekBar) view.findViewById(R.id.sbFileSizeMin);
        mSbFileSizeMax = (SeekBar) view.findViewById(R.id.sbFileSizeMax);
        updateInitialUI();
        mSbFileSizeMin.setOnSeekBarChangeListener(this);
        mSbFileSizeMax.setOnSeekBarChangeListener(this);
        return view;

    }

    private void updateInitialUI() {
        mTvMinFileSize.setText(Formater.formatFileSizeHumanReadable(mMinFileSize));
        mTvMaxFileSize.setText(Formater.formatFileSizeHumanReadable(mMaxFileSize));
        mTvMaxDate.setText(Formater.formatDateShort(mMaxDate * 1000));
        mTvMinDate.setText(Formater.formatDateShort(mMinDate * 1000));
        mSbFileSizeMin.setMax((int) (mMaxFileSize - mMinFileSize));
        mSbFileSizeMax.setMax((int) (mMaxFileSize - mMinFileSize));

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
            mFilterData.mFileSizeMin = (int) (mSbFileSizeMin.getProgress() + mMinFileSize);
            mFilterData.mFileSizeMax = (int) (mSbFileSizeMax.getProgress() + mMinFileSize);
            mListener.onSuccess(mFilterData);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == mSbFileSizeMax) {

            mTvMaxFileSize.setText(Formater.formatFileSizeHumanReadable(progress + mMinFileSize));
        }
        if (seekBar == mSbFileSizeMin) {
            mTvMinFileSize.setText(Formater.formatFileSizeHumanReadable(progress + mMinFileSize));
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        if (seekBar == mSbFileSizeMin && mSbFileSizeMin.getProgress() > mSbFileSizeMax.getProgress())
            mSbFileSizeMax.setProgress(mSbFileSizeMin.getProgress());
        else {
            if (seekBar == mSbFileSizeMax && mSbFileSizeMax.getProgress() < mSbFileSizeMin.getProgress())
                mSbFileSizeMin.setProgress(mSbFileSizeMax.getProgress());
        }

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
