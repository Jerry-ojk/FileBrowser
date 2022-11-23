package jerry.filebrowser.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Scroller
import androidx.core.view.isVisible
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 侧滑删除布局
 */
class SlideLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {
    private var contentView: View? = null
    private var slideView: View? = null

    private val scroller = Scroller(context)

    private var maxSlideDis: Int = 0
    private var downX: Float = 0.0f
    private var lastX: Float = 0.0f
    private val touchSlop = ViewConfiguration.get(getContext()).scaledTouchSlop / 2

    private var state = STATE_CLOSED

    companion object {
        const val STATE_CLOSED = 0
        const val STATE_SLIDING = 1
        const val STATE_OPENING = 2
        const val STATE_CLOSING = 3
        const val STATE_OPENED = 4
    }


    public fun setContentView(view: View) {
        contentView = view
        addView(contentView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    public fun setSlideView(view: View, params: LayoutParams) {
        slideView = view
        addView(slideView, params)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
//        if (childCount >= 1) {
//            setContentView(getChildAt(0))
//        }
//        if (childCount >= 2) {
//            setSlideView(getChildAt(1))
//        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxWidth = 0
        var maxHeight = 0
        if (contentView != null && contentView!!.isVisible) {
            measureChild(contentView, widthMeasureSpec, heightMeasureSpec)
            maxWidth = contentView!!.measuredWidth
            maxHeight = contentView!!.measuredHeight
        }

        if (slideView != null && slideView!!.isVisible) {
            measureChild(
                slideView,
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY)
            )
            maxSlideDis = slideView!!.measuredWidth
            if (slideView!!.measuredWidth > maxWidth) maxWidth = slideView!!.measuredWidth
            if (slideView!!.measuredHeight > maxHeight) maxHeight = slideView!!.measuredHeight
        }

        var width = maxWidth
        var height = maxHeight
        when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> {

            }
            MeasureSpec.EXACTLY -> {
                width = MeasureSpec.getSize(widthMeasureSpec)
            }
            MeasureSpec.AT_MOST -> {
                width = min(maxWidth, MeasureSpec.getSize(widthMeasureSpec))
            }
        }
        when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> {

            }
            MeasureSpec.EXACTLY -> {
                height = MeasureSpec.getSize(heightMeasureSpec)
            }
            MeasureSpec.AT_MOST -> {
                height = min(maxHeight, MeasureSpec.getSize(heightMeasureSpec))
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var left = 0
        for (i in 0 until childCount) {
            val item = getChildAt(i)
            if (item.visibility == View.GONE) continue
            item.layout(
                left,
                0,
                left + item.measuredWidth,
                item.measuredHeight
            )
            left += item.measuredWidth
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        Log.i("onInterceptTouchEvent", "${event?.toString()}")
//        Log.i("onInterceptTouchEvent", "before $state")
        if (state == STATE_SLIDING) {
            parent.requestDisallowInterceptTouchEvent(true)
            return true
        }

        if (state == STATE_OPENING || state == STATE_CLOSING) {
            if (state == STATE_OPENING) {
                scrollTo(maxSlideDis, 0)
            } else {
                scrollTo(0, 0)
            }
        }

        if (super.onInterceptTouchEvent(event)) return true
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                lastX = event.rawX
//                if (state != STATE_CLOSED) {
//                    parent.requestDisallowInterceptTouchEvent(true)
//                    Log.i("onInterceptTouchEvent", "true")
//                    return true
//                }
            }
            MotionEvent.ACTION_MOVE -> {
                val curDis = event.rawX - downX // 本次右滑距离
//                Log.i("onInterceptTouchEvent", "MOVE, $curDis")
                if (state == STATE_OPENED) {
                    if (curDis > touchSlop) {
                        state = STATE_SLIDING
                    }
                } else if (state == STATE_CLOSED) {
                    if (curDis < -touchSlop) {
                        state = STATE_SLIDING
                    }
                }
            }
        }
        if (state != STATE_OPENED) parent.requestDisallowInterceptTouchEvent(true)
        Log.i("onInterceptTouchEvent", "${state != STATE_OPENED}, $state")
        return state != STATE_OPENED
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.i("onTouchEvent", "$event")
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                lastX = event.rawX
                if (state != STATE_OPENED) return true
            }
            MotionEvent.ACTION_MOVE -> {
                val curDis = event.rawX - lastX // 本次右滑距离
//                Log.i("onTouchEvent", "MOVE $curDis")
                if (state == STATE_SLIDING) {
                    val dis = scrollX - curDis // 一共右滑距离
                    if (dis < 0) {
                        state = STATE_CLOSED
                        scrollTo(0, 0)
                    } else if (dis >= maxSlideDis) {
                        state = STATE_OPENED
                        scrollTo(maxSlideDis, 0)
                    } else {
                        scrollTo(dis.toInt(), 0)
                    }
                    lastX = event.rawX
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                } else {
                    if (state == STATE_OPENED) {
                        if (curDis > touchSlop) {
                            state = STATE_SLIDING
                            scrollTo(max(scrollX - curDis.roundToInt(), 0), 0)
                            parent.requestDisallowInterceptTouchEvent(true)
                            return true
                        }
                    } else {
                        if (curDis < -touchSlop) {
                            state = STATE_SLIDING
                            scrollTo(min(-curDis.roundToInt(), maxSlideDis), 0)
                            parent.requestDisallowInterceptTouchEvent(true)
                            return true
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                if (state == STATE_SLIDING) {
                    if (scrollX >= maxSlideDis / 2) {
                        state = STATE_OPENING
                        scroller.startScroll(scrollX, 0, maxSlideDis - scrollX, 0, 250)
                    } else {
                        state = STATE_CLOSING
                        scroller.startScroll(scrollX, 0, -scrollX, 0, 250)
                    }
                    invalidate()
                    return true
                }
            }
        }
//        parent.requestDisallowInterceptTouchEvent(false)
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            if (scroller.currX == maxSlideDis) {
                state = STATE_OPENED
            } else if (scroller.currX == 0) {
                state = STATE_CLOSED
            }
            invalidate()
        }
    }
}