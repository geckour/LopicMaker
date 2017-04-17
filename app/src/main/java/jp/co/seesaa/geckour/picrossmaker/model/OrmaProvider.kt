package jp.co.seesaa.geckour.picrossmaker.model

import android.content.Context

object OrmaProvider {
    lateinit var db: OrmaDatabase

    fun init(context: Context) {
        this.db = OrmaDatabase.builder(context).build()
    }
}