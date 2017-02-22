package jp.co.seesaa.geckour.picrossmaker.model

import android.util.Log
import java.util.*

class KeyStates(val lineSize: Int, val keys: List<Int>) {
    companion object {
        var varCount = 0
    }

    val actualKeys = keys.filter { it > 0 }
    val preKeysSumList: ArrayList<Int> = ArrayList()
    var slideMargin = 0
    var cnfVars: ArrayList<Int> = ArrayList()

    init {
        var keysSum = 0
        for (key in actualKeys) {
            preKeysSumList.add(keysSum)
            keysSum += key
        }

        slideMargin = lineSize - keysSum - keys.size + 1

        (0..(slideMargin + 1) * actualKeys.size - 1).forEach {
            cnfVars.add(++varCount)
        }
    }

    fun getCnfVar(keyIndex: Int, slideIndex: Int): Int? {
        val index = keyIndex * (slideMargin + 1) + slideIndex
        return if (index < cnfVars.size) cnfVars[index] else null
    }

    fun getPreKeysSum(keyIndex: Int): Int? {
        return if (keyIndex < preKeysSumList.size) preKeysSumList[keyIndex] else null
    }
}