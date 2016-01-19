package de.qabel.qabelbox.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import de.qabel.qabelbox.helper.FontHelper;

/**
 * Created by danny on 12.01.2016.
 */
public class SquareTextViewFont extends TextViewFont {

    public SquareTextViewFont(Context context) {
        super(context);

    }

    public SquareTextViewFont(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public SquareTextViewFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SquareTextViewFont(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int dimension = Math.max(width, height);

        setMeasuredDimension(dimension, dimension);
    }
}
