package de.qabel.qabelbox.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import de.qabel.qabelbox.helper.FontHelper;

public class TextViewQabelFont extends TextView {
    public TextViewQabelFont(Context context) {
        super(context);
        FontHelper.getInstance().setCustomeFonts(this);
    }

    public TextViewQabelFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontHelper.getInstance().setQabelFont(this);
    }

    public TextViewQabelFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FontHelper.getInstance().setQabelFont(this);
    }
}
