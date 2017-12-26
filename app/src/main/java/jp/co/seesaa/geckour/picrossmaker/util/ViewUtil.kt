package jp.co.seesaa.geckour.picrossmaker.util

import android.support.design.widget.Snackbar
import android.view.View
import jp.co.seesaa.geckour.picrossmaker.R

fun showSnackbar(view: View, resId: Int, actionStringResId: Int? = null, length: Int = Snackbar.LENGTH_SHORT, action: (View) -> Unit = {}) =
            Snackbar.make(view, resId, length).apply {
                actionStringResId?.apply { setAction(this, action) }
            }.show()

fun showSnackbar(view: View, message: String, actionStringResId: Int? = null, length: Int = Snackbar.LENGTH_SHORT, action: (View) -> Unit = {}) =
        Snackbar.make(view, message, length).apply {
            actionStringResId?.apply { setAction(this, action) }
        }.show()

fun getStrResIdFromStatusCode(statusCode: String) =
        when (statusCode) {
            "1001" -> R.string.api_message_accepted
            "2001" -> R.string.api_message_no_such_problem
            "2002" -> R.string.api_message_unsolvable
            "2003" -> R.string.api_message_invalid_request
            "3001" -> R.string.api_message_unknown_error
            else -> R.string.api_message_unknown_error
        }