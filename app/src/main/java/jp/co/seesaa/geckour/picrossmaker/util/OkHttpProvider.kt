package jp.co.seesaa.geckour.picrossmaker.util

import com.facebook.stetho.okhttp3.StethoInterceptor
import jp.co.seesaa.geckour.picrossmaker.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object OkHttpProvider {

    lateinit var client: OkHttpClient
    // val authInterceptor = AuthInterceptor() // TODO: 認証を実装したらよしなにする

    fun init() {
        val builder = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                addNetworkInterceptor(StethoInterceptor())
            }

            // addNetworkInterceptor(authInterceptor) // TODO: 認証を実装したらよしなにする
        }

        client = builder.build()
    }
}
