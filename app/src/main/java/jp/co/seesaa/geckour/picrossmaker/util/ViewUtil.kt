package jp.co.seesaa.geckour.picrossmaker.util

import android.support.design.widget.Snackbar
import android.view.View

object ViewUtil {
    fun showSnackbar(view: View, resId: Int, actionStringResId: Int? = null, action: (View) -> Unit = {}) =
            Snackbar.make(view, resId, Snackbar.LENGTH_SHORT)
                    .apply {
                        actionStringResId?.apply { setAction(this, action) }
                    }.show()

    fun showSnackbar(view: View, message: String, actionStringResId: Int? = null, action: (View) -> Unit = {}) =
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                    .apply {
                        actionStringResId?.apply { setAction(this, action) }
                    }.show()
}