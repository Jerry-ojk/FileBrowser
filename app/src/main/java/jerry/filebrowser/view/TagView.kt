package jerry.filebrowser.view

import android.content.Context
import kotlin.jvm.JvmOverloads
import android.view.ViewGroup
import jerry.filebrowser.R
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import java.lang.StringBuilder

class TagView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private var enableProcess = false
    private var process = 0
    private val offset: Float
    private val titleColor: Int
    private val subtitleColor: Int
    private val subtitleSize = DPUtils.toDp(context, 10)
    private var title: String? = null
    private var message: String? = null
    private val ivIcon: ImageView
    private val paint: Paint
    private val builder: StringBuilder
    var data: String? = null

    init {
        ivIcon = ImageView(context)
        ivIcon.elevation = DPUtils.DP2.toFloat()

        titleColor = context.getColor(R.color.text_title)
        subtitleColor = context.getColor(R.color.text_subtitle)
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TagView)
            val icon = typedArray.getDrawable(R.styleable.TagView_icon)
            val iconBackground = typedArray.getDrawable(R.styleable.TagView_iconBackground)
            title = typedArray.getString(R.styleable.TagView_title)
            message = typedArray.getString(R.styleable.TagView_message)
            enableProcess = typedArray.getBoolean(R.styleable.TagView_enableProcess, false)
            typedArray.recycle()
            if (icon != null) {
                ivIcon.setImageDrawable(icon)
            }
            if (iconBackground == null) {
                ivIcon.background = ContextCompat.getDrawable(context, R.drawable.tag_bg_light)
            } else {
                ivIcon.background = iconBackground
            }
        } else {
            ivIcon.background = ContextCompat.getDrawable(context, R.drawable.tag_bg_light)
        }
        if (title == null) title = ""
        if (message == null) message = ""
        addView(ivIcon, -1, LayoutParams(DPUtils.DP36, DPUtils.DP36))
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 5f
        paint.textSize = subtitleSize
        val fontMetrics = paint.fontMetrics
        offset = (fontMetrics.bottom - fontMetrics.top - 5) / 2 - fontMetrics.descent
        builder = StringBuilder(4)
        setBackgroundResource(R.drawable.ripple)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChild(ivIcon, widthMeasureSpec, heightMeasureSpec)
        ivIcon.measure(
            MeasureSpec.makeMeasureSpec(DPUtils.DP36, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(DPUtils.DP36, MeasureSpec.EXACTLY)
        )
        if (enableProcess) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), DPUtils.DP60)
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), DPUtils.DP52)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (enableProcess) {
            ivIcon.layout(DPUtils.DP8, DPUtils.DP12, DPUtils.DP44, DPUtils.DP48)
        } else {
            ivIcon.layout(DPUtils.DP8, DPUtils.DP8, DPUtils.DP44, DPUtils.DP44)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val titleSize = DPUtils.DP16.toFloat()
        if (enableProcess) {
            paint.color = titleColor
            paint.textSize = titleSize
            // 绘制标题
            if (title != null) canvas.drawText(
                title!!, DPUtils.DP52.toFloat(), DPUtils.DP20.toFloat(), paint
            )
            //进度
            paint.textSize = subtitleSize
            paint.color = -0XEE8401

            val end = width - DPUtils.DP16

            builder.setLength(0)
            builder.append(process)
            builder.append('%')
            val text = builder.toString()

            val textWidth = paint.measureText(text, 0, text.length).toInt()
            val width = end - DPUtils.DP52 - textWidth - DPUtils.DP6
            val front = width / 100f * process + DPUtils.DP52
            canvas.drawLine(
                DPUtils.DP52.toFloat(), DPUtils.DP32.toFloat(), front, DPUtils.DP32.toFloat(), paint
            )
            canvas.drawCircle(front, DPUtils.DP32.toFloat(), 8f, paint)
            canvas.drawText(text, front + DPUtils.DP6, DPUtils.DP32 + offset, paint)
            //canvas.drawLine(DPUtils.DP52 + front + twidth + DPUtils.DP8, height2, end, height2, paint);

            // 子标题
            paint.color = subtitleColor
            canvas.drawText(message!!, DPUtils.DP52.toFloat(), DPUtils.DP50.toFloat(), paint)
        } else {
            paint.color = titleColor
            paint.textSize = titleSize
            canvas.drawText(
                title!!, DPUtils.DP52.toFloat(), (DPUtils.DP18 + DPUtils.DP2).toFloat(), paint
            )

            paint.color = subtitleColor
            paint.textSize = subtitleSize
            canvas.drawText(message!!, DPUtils.DP52.toFloat(), DPUtils.DP40.toFloat(), paint)
        }
    }

    fun setOnClick(listener: OnClickListener?, drawerLayout: DrawerLayout) {
        if (listener == null) {
            super.setOnClickListener(null)
            return
        }
        super.setOnClickListener { v: View? ->
            drawerLayout.closeDrawer(Gravity.LEFT)
            postDelayed({ listener.onClick(this@TagView) }, 220)
        }
    }

    fun setIcon(@DrawableRes id: Int) {
        ivIcon.setImageResource(id)
    }

    fun setProcess(process: Int) {
        enableProcess = process >= 0
        if (this.process != process) {
            this.process = process
            invalidate()
        }
    }

    fun setTitle(title: String?) {
        this.title = title
    }

    fun setMessage(message: String?) {
        this.message = message
        if (message == null) this.message = ""
        invalidate()
    }

    fun getMessage(): String? {
        return message
    }
}