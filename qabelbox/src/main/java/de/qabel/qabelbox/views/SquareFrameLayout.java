package de.qabel.qabelbox.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by danny on 12.01.2016.
 */
public class SquareFrameLayout extends FrameLayout {

    public SquareFrameLayout(Context context) {

        super(context);
    }

    public SquareFrameLayout(Context context, AttributeSet attrs) {

        super(context, attrs);
    }

    public SquareFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);
    }

    /*
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SquareFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        super(context, attrs, defStyleAttr, defStyleRes);
    }*/

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int dimension = Math.max(width, height);
        setMeasuredDimension(dimension, dimension);


    }
}
