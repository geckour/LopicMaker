package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.Bitmap
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.App
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.api.model.Problem as APIProblem
import jp.co.seesaa.geckour.picrossmaker.model.Problem as DBProblem
import jp.co.seesaa.geckour.picrossmaker.model.Problem.KeysCluster
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.sql.Timestamp
import kotlin.coroutines.experimental.CoroutineContext

fun DBProblem.parse(): APIProblem =
        APIProblem(
                title = this.title,
                genres = this.genres,
                keysHorizontal = this.keysHorizontal.keys.toList(),
                keysVertical = this.keysVertical.keys.toList(),
                thumb = App.Companion.gson.toJson(this.thumb),
                createdAt = this.createdAt.time,
                editedAt = this.editedAt.time
        )

fun APIProblem.parse(): DBProblem =
        DBProblem(
                title = this.title,
                genres = this.genres,
                keysHorizontal = KeysCluster(*this.keysHorizontal.toTypedArray()),
                keysVertical = KeysCluster(*this.keysVertical.toTypedArray()),
                thumb = App.gson.fromJson(this.thumb, Bitmap::class.java),
                createdAt = Timestamp(this.createdAt),
                editedAt = Timestamp(this.editedAt)
        )

fun RxFragment.mainActivity(): MainActivity? = activity as? MainActivity

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