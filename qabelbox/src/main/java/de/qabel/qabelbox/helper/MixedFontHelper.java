package de.qabel.qabelbox.helper;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.array;

public class MixedFontHelper {

    private Typeface[] fonts;
    private static MixedFontHelper instance;

    private MixedFontHelper() {
        // Exists only to defeat instantiation.
    }

    public static MixedFontHelper getInstance() {

        if (instance == null) {
            instance = new MixedFontHelper();
        }
        return instance;
    }

    private void loadCustomeFonts(Context context) {

        //load fonts in fixed order, 0=normal, 1=bold, 2=italic, 3=bolditalic
        String[] fontList = context.getResources().getStringArray(array.fonts_with_qabel);
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
            int style = Typeface.NORMAL;
            if (view.getTypeface() != null) {
                style = view.getTypeface().getStyle();
            }
            if (style >= 0 && style < fonts.length) {
                view.setTypeface(fonts[style], style);
            }
        }
    }
}
