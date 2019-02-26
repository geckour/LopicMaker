package com.geckour.lopicmaker.util

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.geckour.lopicmaker.R

@BindingAdapter("app:tagList")
fun getTagsText(view: View, tags: List<String>) {
    view.context.getString(
        R.string.problem_fragment_text_tag_prefix,
        if (tags.isEmpty()) "-" else tags.joinToString(", ")
    )
}

@BindingAdapter("app:srcBitmap")
fun loadImage(imageView: ImageView, bitmap: Bitmap?) {
    imageView.setImageBitmap(bitmap)
}