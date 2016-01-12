package de.qabel.qabelbox.helper;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import de.qabel.qabelbox.QabelBoxApplication;
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
            instance.loadCustomeFonts(context);
        }
        return instance;
    }

    private void loadCustomeFonts(Context context) {
        String[] fontList = context.getResources().getStringArray(R.array.fonts);
        fonts = new Typeface[fontList.length];

        for (int i = 0; i < fontList.length; i++) {
            fonts[i] = Typeface.createFromAsset(context.getAssets(), fontList[i]);
         }
    }

    public void setCustomeFonts(TextView view) {
        //if(!view.isInEditMode())
        {
            view.setTypeface(fonts[0], Typeface.NORMAL);
            view.setTypeface(fonts[1], Typeface.ITALIC);
            view.setTypeface(fonts[2], Typeface.BOLD);
        }
    }


}
