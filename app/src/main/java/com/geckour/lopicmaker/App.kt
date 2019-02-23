package com.geckour.lopicmaker

import android.app.Application
import com.facebook.stetho.Stetho
import com.geckour.lopicmaker.util.OkHttpProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import timber.log.Timber

class App : Application() {

    companion object {
        val gson: Gson
            get() =
                GsonBuilder().apply {
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

        OkHttpProvider.init()
    }
}