package jerry.filebrowser.dialog

import android.content.Context
import jerry.filebrowser.R

class VideoInfoDialog(context: Context) : BaseDialog(context) {
    override fun getLayoutId(): Int {
        return R.layout.dialog_edit
    }
}