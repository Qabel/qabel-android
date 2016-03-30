package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.SeekBar;
import android.widget.TextView;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.Formatter;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.StorageSearch;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;

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
    long mNewMinDate = 0;
    long mNewMaxDate = 0;

    private SeekBar mSbFileSizeMin;
    private SeekBar mSbFileSizeMax;
    private TextView btSelectMinDate;
    private TextView btSelectMaxDate;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_files_search_filter, container, false);
        mTvMinFileSize = (TextView) view.findViewById(R.id.tvMinFileSize);
        mTvMaxFileSize = (TextView) view.findViewById(R.id.tvMaxFileSize);
        mTvMinDate = (TextView) view.findViewById(R.id.tvMinDate);
        mTvMaxDate = (TextView) view.findViewById(R.id.tvMaxDate);
        mSbFileSizeMin = (SeekBar) view.findViewById(R.id.sbFileSizeMin);
        mSbFileSizeMax = (SeekBar) view.findViewById(R.id.sbFileSizeMax);
        btSelectMinDate = (TextView) view.findViewById(R.id.btMinDate);
        btSelectMaxDate = (TextView) view.findViewById(R.id.btMaxDate);
        updateInitialUI();
        mSbFileSizeMin.setOnSeekBarChangeListener(this);
        mSbFileSizeMax.setOnSeekBarChangeListener(this);
        setDateButtonClickListener();
        return view;
    }

    private void setDateButtonClickListener() {
        btSelectMinDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment newFragment = DatePickerFragment.newInstance(mMinDate, mMaxDate, mMinDate, new DatePickerFragment.CallbackListener() {
                    @Override
                    public void onSuccess(int year, int month, int day) {
                        mMinDate = getStartOfDay(getDate(year, month, day)).getTime() / 1000;
                        mNewMinDate = mMinDate;
                        mTvMinDate.setText(Formatter.formatDateShort(mMinDate * 1000));
                        if (mMinDate > mMaxDate) {
                            mMaxDate = getEndOfDay(getDate(year, month, day)).getTime() / 1000;
                            mNewMaxDate = mMaxDate;
                            mTvMaxDate.setText(Formatter.formatDateShort(mMaxDate * 1000));

                        }
                    }
                });
                newFragment.show(getFragmentManager(), "datePicker");
            }
        });
        btSelectMaxDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment newFragment = DatePickerFragment.newInstance(mMinDate, mMaxDate, mMaxDate, new DatePickerFragment.CallbackListener() {
                    @Override
                    public void onSuccess(int year, int month, int day) {
                        mMaxDate = getEndOfDay(getDate(year, month, day)).getTime() / 1000;
                        mNewMaxDate = mMaxDate;
                        mTvMaxDate.setText(Formatter.formatDateShort(mMaxDate * 1000));
                        if (mMaxDate < mMinDate) {
                            mMinDate = getStartOfDay(getDate(year, month, day)).getTime() / 1000;
                            mNewMinDate = mMinDate;
                            mTvMinDate.setText(Formatter.formatDateShort(mMinDate * 1000));
                        }

                    }
                });
                newFragment.show(getFragmentManager(), "datePicker");
            }
        });
    }

    private Date getDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c.getTime();
    }

    public static Date getEndOfDay(Date date) {
        return DateUtils.addMilliseconds(DateUtils.ceiling(date, Calendar.DATE), -1);
    }

    public static Date getStartOfDay(Date date) {
        return DateUtils.truncate(date, Calendar.DATE);
    }

    private void updateInitialUI() {
        mTvMinFileSize.setText(Formatter.formatFileSizeHumanReadable(getActivity(), mMinFileSize));
        mTvMaxFileSize.setText(Formatter.formatFileSizeHumanReadable(getActivity(), mMaxFileSize));
        if (mFilterData.mDateMax != null) {
            mTvMaxDate.setText(Formatter.formatDateShort(mFilterData.mDateMax));
        } else {
            mTvMaxDate.setText(Formatter.formatDateShort(mMaxDate * 1000));
        }
        if (mFilterData.mDateMin != null) {
            mTvMinDate.setText(Formatter.formatDateShort(mFilterData.mDateMin));
        } else {
            mTvMinDate.setText(Formatter.formatDateShort(mMinDate * 1000));
        }
        mSbFileSizeMin.setMax((int) (mMaxFileSize - mMinFileSize));
        mSbFileSizeMax.setMax((int) (mMaxFileSize - mMinFileSize));
        mSbFileSizeMin.setProgress((int) (mFilterData.mFileSizeMin - mMinFileSize));
        mSbFileSizeMax.setProgress((int) (mFilterData.mFileSizeMax - mMinFileSize));
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
            if (mNewMinDate > 0) {
                mFilterData.mDateMin = new Date(mNewMinDate * 1000);
            }
            if (mNewMaxDate > 0) {
                mFilterData.mDateMax = new Date(mNewMaxDate * 1000);
            }
            mListener.onSuccess(mFilterData);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == mSbFileSizeMax) {

            mTvMaxFileSize.setText(Formatter.formatFileSizeHumanReadable(getActivity(), progress + mMinFileSize));
        }
        if (seekBar == mSbFileSizeMin) {
            mTvMinFileSize.setText(Formatter.formatFileSizeHumanReadable(getActivity(), progress + mMinFileSize));
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        if (seekBar == mSbFileSizeMin && mSbFileSizeMin.getProgress() > mSbFileSizeMax.getProgress()) {
            mSbFileSizeMax.setProgress(mSbFileSizeMin.getProgress());
        } else {
            if (seekBar == mSbFileSizeMax && mSbFileSizeMax.getProgress() < mSbFileSizeMin.getProgress()) {
                mSbFileSizeMin.setProgress(mSbFileSizeMax.getProgress());
            }
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
