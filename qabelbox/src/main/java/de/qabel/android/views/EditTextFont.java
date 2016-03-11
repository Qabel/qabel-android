package de.qabel.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import de.qabel.android.helper.FontHelper;

/**
 * Created by danny on 12.01.2016.
 */
public class EditTextFont extends EditText {
    public EditTextFont(Context context) {
        super(context);
        FontHelper.getInstance().setCustomeFonts(this);
    }

    public EditTextFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontHelper.getInstance().setCustomeFonts(this);
    }

    public EditTextFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FontHelper.getInstance().setCustomeFonts(this);
    }

    /*
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EditTextFont(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        FontHelper.getInstance(context).setCustomeFonts(this);
    }*/
}
