package de.qabel.qabelbox.contacts.view.widgets

import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.Shape

class IdentityIconDrawable (
        private val textPaint: Paint = Paint(),
        private val text: String = "",
        color: Int = Color.GRAY,
        private val height: Int = -1,
        private val width: Int = -1,
        private val fontSize: Int = -1,
        shape: Shape = OvalShape(),
        font: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL),
        textColor: Int = Color.WHITE,
        isBold: Boolean = false
)
: ShapeDrawable(shape) {


    init {
        textPaint.color = textColor
        textPaint.isAntiAlias = true
        textPaint.isFakeBoldText = isBold
        textPaint.style = Paint.Style.FILL
        textPaint.typeface = font
        textPaint.textAlign = Paint.Align.CENTER

        paint.color = color

    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val r = bounds

        val count = canvas.save()
        canvas.translate(r.left.toFloat(), r.top.toFloat())

        val fontSize = if (this.fontSize < 0) Math.min(width, height) / 2 else this.fontSize
        textPaint.textSize = fontSize.toFloat()
        canvas.drawText(text, (width / 2).toFloat(), height / 2 - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)

        canvas.restoreToCount(count)

    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        textPaint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }

}
