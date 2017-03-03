package jp.co.seesaa.geckour.picrossmaker.model

import android.util.Log
import java.util.*

class KeyStates(val lineSize: Int, val keys: List<Int>) {
    companion object {
        var varCount = 0
    }

    val actualKeys = keys.filter { it > 0 }
    private val preKeysSumList: ArrayList<Int> = ArrayList()
    val slideMargin: Int
    private val cnfVars: ArrayList<Int> = ArrayList()

    init {
        var keysSum = 0
        for (key in actualKeys) {
            this.preKeysSumList.add(keysSum)
            keysSum += key
        }

        this.slideMargin = this.lineSize - keysSum - this.keys.size + 1

        (0..(this.slideMargin + 1) * this.actualKeys.size - 1).forEach {
            this.cnfVars.add(++varCount)
        }
    }

    fun getCnfVar(keyIndex: Int, slideIndex: Int): Int? {
        val index = keyIndex * (slideMargin + 1) + slideIndex
        return if (-1 < index && index < cnfVars.size) cnfVars[index] else null
    }

    fun getPreKeysSum(keyIndex: Int): Int? {
        return if (-1 < keyIndex && keyIndex < preKeysSumList.size) preKeysSumList[keyIndex] else null
    }
}