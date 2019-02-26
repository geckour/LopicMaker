package com.geckour.lopicmaker.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geckour.lopicmaker.data.DB
import com.geckour.lopicmaker.data.model.Problem
import com.geckour.lopicmaker.util.SingleLiveEvent
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    enum class FabRightMode {
        ADD,
        EDIT,
        SCALE
    }

    enum class FabLeftMode {
        UNDO,
        REDO,
        FILL,
        MARK_NOT_FILL
    }

    internal val fabRightMode = SingleLiveEvent<FabRightMode>()
    internal val fabRightVisible = SingleLiveEvent<Boolean>()
    internal val fabLeftMode = SingleLiveEvent<FabLeftMode>()
    internal val fabLeftVisible = SingleLiveEvent<Boolean>()
    internal val fabLeftClicked = SingleLiveEvent<FabLeftMode>()

    internal val toolbarTitleResId = SingleLiveEvent<Pair<@androidx.annotation.StringRes Int, List<String>>>()

    internal val snackBarStringResId = SingleLiveEvent<@androidx.annotation.StringRes Int>()

    internal val toSolveProblemId = SingleLiveEvent<Long>()
    internal val toEditProblemId = SingleLiveEvent<Long>()
    internal val toEditDraftProblemId = SingleLiveEvent<Long>()

    internal fun deleteProblem(context: Context, problem: Problem, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = DB.getInstance(context).problemDao().delete(problem) > 0
            onComplete(result)
        }
    }
}