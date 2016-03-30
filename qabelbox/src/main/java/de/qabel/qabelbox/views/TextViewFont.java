package de.qabel.qabelbox.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import de.qabel.qabelbox.helper.FontHelper;

/**
 * Created by danny on 12.01.2016.
 */
public class TextViewFont extends TextView {

    public TextViewFont(Context context) {
        super(context);
        FontHelper.getInstance().setCustomeFonts(this);
    }

    public TextViewFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontHelper.getInstance().setCustomeFonts(this);
    }

    public TextViewFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FontHelper.getInstance().setCustomeFonts(this);
    }
/*
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextViewFont(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        FontHelper.getInstance(context).setCustomeFonts(this);
    }*/


}
