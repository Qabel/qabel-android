package de.qabel.qabelbox.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import de.qabel.qabelbox.helper.MixedFontHelper;

/**
 * This class is hack to be able to mix the Branded QABEL-Font based on Art Post font with regular Robotofonts.
 * <p>
 * To use the Art Post font format your text using a SpannableString and mart it as bolditalic. Caution: The font is limited, only uppercase characters will work.
 * <p>
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 02.03.16.
 */
public class QabelBrandingFont extends TextView {

    public QabelBrandingFont(Context context) {
        super(context);
        MixedFontHelper.getInstance().setCustomeFonts(this);
    }

    public QabelBrandingFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        MixedFontHelper.getInstance().setCustomeFonts(this);
    }

    public QabelBrandingFont(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        MixedFontHelper.getInstance().setCustomeFonts(this);
    }
}
