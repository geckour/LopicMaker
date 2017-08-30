package jp.co.seesaa.geckour.picrossmaker

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider

class App : Application() {

    companion object {
        val gson: Gson = GsonBuilder().apply {}.create()
    }

    override fun onCreate() {
        super.onCreate()

        OrmaProvider.init(this)
    }
}