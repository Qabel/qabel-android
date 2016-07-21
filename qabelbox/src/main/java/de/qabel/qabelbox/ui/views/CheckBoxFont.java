package de.qabel.qabelbox.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

import de.qabel.qabelbox.helper.FontHelper;

public class CheckBoxFont extends CheckBox {
    public CheckBoxFont(Context context) {
        super(context);
        FontHelper.getInstance().setCustomFonts(this);
    }

    public CheckBoxFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontHelper.getInstance().setCustomFonts(this);
    }

    public CheckBoxFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FontHelper.getInstance().setCustomFonts(this);
    }
/*
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CheckBoxFont(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        FontHelper.getInstance(context).setCustomFonts(this);
    }*/
}
