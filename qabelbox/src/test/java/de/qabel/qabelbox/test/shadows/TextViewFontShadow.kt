package de.qabel.qabelbox.test.shadows

import de.qabel.qabelbox.views.TextViewFont
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowView

@Implements(TextViewFont::class)
open class TextViewFontShadow: ShadowView() {

    @RealObject var texttView: TextViewFont? = null

}

