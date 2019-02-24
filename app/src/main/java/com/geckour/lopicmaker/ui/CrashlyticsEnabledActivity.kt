package com.geckour.lopicmaker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crashlytics.android.Crashlytics
import com.geckour.lopicmaker.BuildConfig
import io.fabric.sdk.android.Fabric

abstract class CrashlyticsEnabledActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG.not()) Fabric.with(this, Crashlytics())
    }
}