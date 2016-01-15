package de.qabel.qabelbox.fragments;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Created by danny on 14.01.2016.
 */

public class DatePickerFragment extends DialogFragment implements
        DatePickerDialog.OnDateSetListener {
    private CallbackListener mCallback;
    private long mMaxDate;
    private long mMinDate;
    private long mCurrentDate;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(mCurrentDate * 1000);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);
        dialog.getDatePicker().setMaxDate(mMaxDate * 1000);
        dialog.getDatePicker().setMinDate(mMinDate * 1000);
        return dialog;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Log.d("DatePicker", "Date = " + year + " " + month + " " + day);
        mCallback.onSuccess(year, month, day);
    }

    public static DatePickerFragment newInstance(long mMinDate, long mMaxDate, long currentDate, CallbackListener callbackListener) {
        DatePickerFragment fragment = new DatePickerFragment();
        fragment.mCallback = callbackListener;
        fragment.mMinDate = mMinDate;
        fragment.mMaxDate = mMaxDate;
        fragment.mCurrentDate = currentDate;
        return fragment;
    }

    public interface CallbackListener {
        void onSuccess(int year, int month, int day);
    }
}

