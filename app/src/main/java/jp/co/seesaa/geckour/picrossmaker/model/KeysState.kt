package jp.co.seesaa.geckour.picrossmaker.model

import java.util.*

class KeysState(val lineSize: Int, val keys: List<Int>) {
    companion object {
        var varCount = 0
    }

    val preKeysSumList: ArrayList<Int> = ArrayList()
    var slideMargin = 0
    var cnfVars: ArrayList<Int> = ArrayList()

    init {
        setCnfVars()
    }

    fun getCnfVar(keyIndex: Int, slideIndex: Int): Int? {
        val index = keyIndex * (slideMargin + 1) + slideIndex
        return if (index < cnfVars.size) cnfVars[index] else null
    }

    fun setCnfVars() {
        var keysSum = 0
        for (key in keys) {
            preKeysSumList.add(keysSum)
            keysSum += key
        }

        slideMargin = lineSize - keysSum - keys.size + 1

        for (i in 0..(slideMargin + 1) * keys.size - 1) {
            cnfVars.add(++varCount)
        }
    }

    fun getPreKeysSum(keyIndex: Int): Int? {
        return if (keyIndex < preKeysSumList.size) preKeysSumList[keyIndex] else null
    }
}