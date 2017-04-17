package jp.co.seesaa.geckour.picrossmaker

import android.app.Application
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        OrmaProvider.init(this)
    }
}