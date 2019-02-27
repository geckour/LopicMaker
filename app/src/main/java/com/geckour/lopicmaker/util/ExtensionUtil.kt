package com.geckour.lopicmaker.util

import com.crashlytics.android.Crashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.sat4j.specs.TimeoutException

inline fun <reified T> Gson.fromJson(json: String): T = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun String.getTagsList(): List<String> =
    this.split(" ")

val UniqueSolutionCounter.solutionCount: Long
    get() =
        try {
            countSolutions()
        } catch (t: TimeoutException) {
            Crashlytics.logException(t)
            lowerBound()
        } finally {
            -1
        }