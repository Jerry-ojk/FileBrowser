package jerry.filebrowser.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jerry.filebrowser.R
import jerry.filebrowser.adapter.PathNavAdapter

class PathNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    var path: String = ""
        set(value) {
            field = value
            adapter.updatePath(value)
            post(runnable)
        }

    private val adapter: PathNavAdapter = PathNavAdapter(this)
    private val runnable = Runnable { scrollToPosition(adapter.itemCount - 1) }

    init {
        addItemDecoration(ArrowItemDecoration(context))
        val layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        setLayoutManager(layoutManager)
        setAdapter(adapter)
    }

    fun setOnPathClickListener(onPathClickListener: OnPathClickListener?) {
        adapter.setOnPathClickListener(onPathClickListener)
    }

    fun setLoading(isLoading: Boolean) {
        adapter.setLoading(isLoading)
    }

    interface OnPathClickListener {
        fun onNavDirectory(absPath: String?, type: Int)
    }

    class ArrowItemDecoration(context: Context) : ItemDecoration() {
        private val arrow: Drawable =
            ContextCompat.getDrawable(context, R.drawable.ic_action_next)!!
        private val width: Int = DPUtils.DP(24)

        init {
            arrow.setTint(context.getColor(R.color.text_subtitle))
        }

        override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
            val top = (parent.height - width) / 2
            val count = parent.childCount - 1
            for (i in 0 until count) {
                val left = parent.getChildAt(i + 1).left - width + DPUtils.DP4
                arrow.setBounds(left, top, left + width, top + width)
                arrow.draw(canvas)
            }
        }

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            // int position = parent.getChildAdapterPosition(view);
            // int count = parent.getAdapter().getItemCount();
            outRect[0, 0, DPUtils.DP(16)] = 0
        }
    }
}