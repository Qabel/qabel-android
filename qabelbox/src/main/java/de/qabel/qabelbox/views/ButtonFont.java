package de.qabel.qabelbox.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import de.qabel.qabelbox.helper.FontHelper;

public class ButtonFont extends Button {
    public ButtonFont(Context context) {
        super(context);
        FontHelper.getInstance().setCustomeFonts(this);
    }

    public ButtonFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FontHelper.getInstance().setCustomeFonts(this);
    }


    public ButtonFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontHelper.getInstance().setCustomeFonts(this);
    }

    /*
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ButtonFont(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        FontHelper.getInstance(context).setCustomeFonts(this);
    }*/
}
