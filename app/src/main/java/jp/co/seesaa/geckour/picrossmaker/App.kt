package jp.co.seesaa.geckour.picrossmaker

import android.app.Application
import com.facebook.stetho.Stetho
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.util.OkHttpProvider
import timber.log.Timber

class App : Application() {

    companion object {
        val gson: Gson = GsonBuilder().apply {
            serializeNulls()
            setLenient()
        }.create()
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initializeWithDefaults(this)
        }

        OrmaProvider.init(this)
        OkHttpProvider.init()
    }
}