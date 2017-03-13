package jp.co.seesaa.geckour.picrossmaker.model

import android.util.Log
import java.util.*

class KeyStates(val lineSize: Int, val keys: List<Int>) {
    companion object {
        var varCount = 0
    }

    val slideMargin: Int
    private val preKeysSumList: ArrayList<Int> = ArrayList()
    private val cnfVars: ArrayList<Int> = ArrayList()

    init {
        var keysSum = 0
        for (key in keys) {
            this.preKeysSumList.add(keysSum)
            keysSum += key
        }

        this.slideMargin = this.lineSize - keysSum - this.keys.size + 1

        (0..(this.slideMargin + 1) * this.keys.size - 1).forEach {
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