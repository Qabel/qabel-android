package de.qabel.qabelbox.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import de.qabel.qabelbox.helper.FontHelper;

/**
 * Created by danny on 12.01.2016.
 */
public class EditTextFont extends EditText {
    public EditTextFont(Context context) {
        super(context);
        FontHelper.getInstance().setCustomFonts(this);
    }

    public EditTextFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontHelper.getInstance().setCustomFonts(this);
    }

    public EditTextFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FontHelper.getInstance().setCustomFonts(this);
    }

    /*
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EditTextFont(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        FontHelper.getInstance(context).setCustomFonts(this);
    }*/
}
