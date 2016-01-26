package de.qabel.qabelbox.helper;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 12.01.2016.
 */
public class FontHelper {

    private Typeface[] fonts;
    private static FontHelper instance = null;

    protected FontHelper() {
        // Exists only to defeat instantiation.
    }

    public static FontHelper getInstance(Context context) {

        if (instance == null) {
            instance = new FontHelper();
        }
        return instance;
    }

    private void loadCustomeFonts(Context context) {

        //load fonts in fixed order, 0=normal, 1=bold, 2=italic, 3=bolditalic
        String[] fontList = context.getResources().getStringArray(R.array.fonts);
        fonts = new Typeface[fontList.length];


        for (int i = 0; i < fontList.length; i++) {
            fonts[i] = Typeface.createFromAsset(context.getAssets(), fontList[i]);
        }
    }

    public void setCustomeFonts(TextView view) {

        if (view != null && !view.isInEditMode()) {
            if (fonts == null) {
                loadCustomeFonts(view.getContext());
            }
            int style=Typeface.NORMAL;
            if(view.getTypeface()!=null)
            {
                style = view.getTypeface().getStyle();
            }
            if (style >= 0 && style < fonts.length) {
                view.setTypeface(fonts[style], style);
            }
        }
    }
}
