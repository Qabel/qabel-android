package de.qabel.qabelbox.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.vanniktech.emoji.EmojiTextView;

import de.qabel.qabelbox.helper.FontHelper;

public class TextViewFont extends EmojiTextView {

    public TextViewFont(Context context) {
        super(context);
        FontHelper.getInstance().setCustomFonts(this);
    }

    public TextViewFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontHelper.getInstance().setCustomFonts(this);
    }

    public TextViewFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FontHelper.getInstance().setCustomFonts(this);
    }
/*
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextViewFont(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        FontHelper.getInstance(context).setCustomFonts(this);
    }*/


}
