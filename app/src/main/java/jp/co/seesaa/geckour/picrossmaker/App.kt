package jp.co.seesaa.geckour.picrossmaker

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

class App : Application() {

    companion object {
        val gson: Gson = GsonBuilder().apply {}.create()
    }

    override fun onCreate() {
        super.onCreate()

        OrmaProvider.init(this)
    }
}

fun <T> async(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> T) =
        kotlinx.coroutines.experimental.async(context, block = block)

fun ui(managerList: ArrayList<Job>, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> Unit) =
        launch(UI) {
            try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }.apply {
            managerList.add(this)
        }