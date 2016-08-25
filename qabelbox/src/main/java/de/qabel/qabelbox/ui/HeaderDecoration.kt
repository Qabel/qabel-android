package de.qabel.qabelbox.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.support.annotation.DimenRes
import android.support.annotation.FloatRange
import android.support.annotation.IntRange
import android.support.annotation.LayoutRes
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View

class HeaderDecoration(private val mView: View,
                       private val mHorizontal: Boolean,
                       private val mParallax: Float,
                       private val mShadowSize: Float,
                       private val mColumns: Int) : RecyclerView.ItemDecoration() {

    private val mShadowPaint: Paint?

    init {

        if (mShadowSize > 0) {
            mShadowPaint = Paint()
            mShadowPaint.shader = if (mHorizontal)
                LinearGradient(mShadowSize, 0f, 0f, 0f,
                        intArrayOf(Color.argb(55, 0, 0, 0), Color.argb(55, 0, 0, 0), Color.argb(3, 0, 0, 0)),
                        floatArrayOf(0f, .5f, 1f),
                        Shader.TileMode.CLAMP)
            else
                LinearGradient(0f, mShadowSize, 0f, 0f,
                        intArrayOf(Color.argb(55, 0, 0, 0), Color.argb(55, 0, 0, 0), Color.argb(3, 0, 0, 0)),
                        floatArrayOf(0f, .5f, 1f),
                        Shader.TileMode.CLAMP)
        } else {
            mShadowPaint = null
        }
    }

    companion object {

        fun with(context: Context): Builder {
            return Builder(context)
        }

        fun with(recyclerView: RecyclerView): Builder {
            val layoutManager = recyclerView.layoutManager
            if (layoutManager is GridLayoutManager) {
                return Builder(recyclerView.context, layoutManager.spanCount)
            } else if (layoutManager is LinearLayoutManager) {
                return Builder(recyclerView.context, layoutManager.orientation == LinearLayoutManager.HORIZONTAL)
            } else if (layoutManager is StaggeredGridLayoutManager) {
                return Builder(recyclerView.context, layoutManager.spanCount)
            } else {
                return Builder(recyclerView.context)
            }
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        super.onDraw(c, parent, state)
        // layout basically just gets drawn on the reserved space on top of the first view
        mView.layout(parent.left, 0, parent.right, mView.measuredHeight)

        for (i in 0..parent.childCount - 1) {
            val view = parent.getChildAt(i)
            if (parent.getChildAdapterPosition(view) == 0) {
                c.save()
                if (mHorizontal) {
                    c.clipRect(parent.left, parent.top, view.left, parent.bottom)
                    val width = mView.measuredWidth
                    val left = (view.left - width) * mParallax
                    c.translate(left, 0f)
                    mView.draw(c)
                    if (mShadowSize > 0) {
                        c.translate(view.left.toFloat() - left - mShadowSize, 0f)
                        c.drawRect(parent.left.toFloat(), parent.top.toFloat(), mShadowSize, parent.bottom.toFloat(), mShadowPaint!!)
                    }
                } else {
                    c.clipRect(parent.left, parent.top, parent.right, view.top)
                    val height = mView.measuredHeight
                    val top = (view.top - height) * mParallax
                    c.translate(0f, top)
                    mView.draw(c)
                    if (mShadowSize > 0) {
                        c.translate(0f, view.top.toFloat() - top - mShadowSize)
                        c.drawRect(parent.left.toFloat(), parent.top.toFloat(), parent.right.toFloat(), mShadowSize, mShadowPaint!!)
                    }
                }
                c.restore()
                break
            }
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        if (parent.getChildAdapterPosition(view) < mColumns) {
            if (mHorizontal) {
                if (mView.measuredWidth <= 0) {
                    mView.measure(View.MeasureSpec.makeMeasureSpec(parent.measuredWidth, View.MeasureSpec.AT_MOST),
                            View.MeasureSpec.makeMeasureSpec(parent.measuredHeight, View.MeasureSpec.AT_MOST))
                }
                outRect.set(mView.measuredWidth, 0, 0, 0)
            } else {
                if (mView.measuredHeight <= 0) {
                    mView.measure(View.MeasureSpec.makeMeasureSpec(parent.measuredWidth, View.MeasureSpec.AT_MOST),
                            View.MeasureSpec.makeMeasureSpec(parent.measuredHeight, View.MeasureSpec.AT_MOST))
                }
                outRect.set(0, mView.measuredHeight, 0, 0)
            }
        } else {
            outRect.setEmpty()
        }
    }


    class Builder {
        private var mContext: Context? = null

        private var mView: View? = null
        private var mHorizontal: Boolean = false
        private var mParallax = 1f
        private var mShadowSize: Float = 0.toFloat()
        private var mColumns = 1

        constructor(context: Context) {
            mContext = context
        }

        constructor(context: Context, @IntRange(from = 1) columns: Int) {
            mContext = context
            mColumns = columns
        }

        constructor(context: Context, horizontal: Boolean) {
            mContext = context
            mHorizontal = horizontal
        }


        fun setView(view: View): Builder {
            mView = view
            return this
        }

        fun inflate(@LayoutRes layoutRes: Int): Builder {
            mView = LayoutInflater.from(mContext).inflate(layoutRes, null, false)
            return this
        }

        /**
         * Adds a parallax effect.

         * @param parallax the multiplier to use, 0f would be the view standing still, 1f moves along with the first item.
         * *
         * @return this builder
         */
        fun parallax(@FloatRange(from = 0.0, to = 1.0) parallax: Float): Builder {
            mParallax = parallax
            return this
        }

        fun scrollsHorizontally(isHorizontal: Boolean): Builder {
            mHorizontal = isHorizontal
            return this
        }

        fun dropShadowDp(@IntRange(from = 0, to = 80) dp: Int): Builder {
            mShadowSize = mContext!!.resources.displayMetrics.density * dp
            return this
        }

        fun dropShadow(@DimenRes dimenResource: Int): Builder {
            mShadowSize = mContext!!.resources.getDimensionPixelSize(dimenResource).toFloat()
            return this
        }

        fun build(): HeaderDecoration {
            if (mView == null) {
                throw IllegalStateException("View must be set with either setView or inflate")
            }
            return HeaderDecoration(mView!!, mHorizontal, mParallax, mShadowSize * 1.5f, mColumns)
        }

        fun columns(@IntRange(from = 1) columns: Int): Builder {
            mColumns = columns
            return this
        }
    }
}
