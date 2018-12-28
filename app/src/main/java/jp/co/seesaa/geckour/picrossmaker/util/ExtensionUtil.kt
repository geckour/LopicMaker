package jp.co.seesaa.geckour.picrossmaker.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import jp.co.seesaa.geckour.picrossmaker.model.Problem.KeysCluster
import jp.co.seesaa.geckour.picrossmaker.presentation.activity.MainActivity
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import java.sql.Timestamp
import kotlin.coroutines.experimental.CoroutineContext
import jp.co.seesaa.geckour.picrossmaker.api.model.Problem as APIProblem
import jp.co.seesaa.geckour.picrossmaker.model.Problem as DBProblem

fun DBProblem.parse(): APIProblem =
        APIProblem(
                title = this.title,
                tags = this.tags,
                keysHorizontal = this.keysHorizontal.keys.toList(),
                keysVertical = this.keysVertical.keys.toList(),
                createdAt = this.createdAt.time,
                editedAt = this.editedAt.time
        )

fun APIProblem.parse(algorithm: Algorithm, cells: List<Cell>): DBProblem {
    return DBProblem(
            title = this.title,
            draft = false,
            tags = this.tags,
            keysHorizontal = KeysCluster(*this.keysHorizontal.toTypedArray()),
            keysVertical = KeysCluster(*this.keysVertical.toTypedArray()),
            thumb = algorithm.getThumbnailImage(cells),
            createdAt = Timestamp(this.createdAt),
            editedAt = Timestamp(this.editedAt),
            source = jp.co.seesaa.geckour.picrossmaker.model.Problem.Source.SERVER_ORIGIN
    )
}

fun RxFragment.mainActivity(): MainActivity? = activity as? MainActivity

fun <T> async(context: CoroutineContext = CommonPool, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> T) =
        try {
            kotlinx.coroutines.experimental.async(context, block = block)
        } catch (e: Exception) {
            Timber.e(e)
            onError(e)
            null
        }

fun ui(managerList: ArrayList<Job>, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> Unit) =
        launch(UI) {
            try { block() }
            catch (e: Exception) {
                Timber.e(e)
                onError(e)
            }
        }.apply { managerList.add(this) }

inline fun <reified T> Gson.fromJson(json: String): T = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun String.getTagsList(): List<String> =
        this.split(" ")