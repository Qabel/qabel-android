package de.qabel.qabelbox.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.EditText;

import de.qabel.qabelbox.helper.FontHelper;

/**
 * Created by danny on 12.01.2016.
 */
public class EditTextFont extends EditText {
    public EditTextFont(Context context) {
        super(context);
        FontHelper.getInstance(context).setCustomeFonts(this);
    }

    public EditTextFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontHelper.getInstance(context).setCustomeFonts(this);
    }

    public EditTextFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FontHelper.getInstance(context).setCustomeFonts(this);
    }

    /*
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EditTextFont(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        FontHelper.getInstance(context).setCustomeFonts(this);
    }*/
}
