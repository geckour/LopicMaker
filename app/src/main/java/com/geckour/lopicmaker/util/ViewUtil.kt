package com.geckour.lopicmaker.util

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun showSnackbar(
    view: View,
    resId: Int,
    actionStringResId: Int? = null,
    length: Int = Snackbar.LENGTH_SHORT,
    action: (View) -> Unit = {}
) = Snackbar.make(view, resId, length).apply {
    actionStringResId?.apply { setAction(this, action) }
}.show()

fun showSnackbar(
    view: View,
    message: String,
    actionStringResId: Int? = null,
    length: Int = Snackbar.LENGTH_SHORT,
    action: (View) -> Unit = {}
) = Snackbar.make(view, message, length).apply {
    actionStringResId?.apply { setAction(this, action) }
}.show()