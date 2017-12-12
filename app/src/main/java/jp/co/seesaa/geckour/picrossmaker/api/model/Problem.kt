package jp.co.seesaa.geckour.picrossmaker.api.model

import com.google.gson.annotations.SerializedName

data class Problem(
        val id: Long? = null,

        val title: String,

        val tags: List<String>,

        @SerializedName("keys_horizontal")
        val keysHorizontal: List<List<Int>>,

        @SerializedName("keys_vertical")
        val keysVertical: List<List<Int>>,

        @SerializedName("created_at")
        val createdAt: Long = System.currentTimeMillis(),

        @SerializedName("edited_at")
        val editedAt: Long = System.currentTimeMillis()
)