package com.geckour.lopicmaker.domain.model

data class KeyStates(val lineSize: Int, val keys: List<Int>) {
    companion object {
        var varCount = 0
    }

    private val preKeysSumList: List<Int> = keys.mapIndexed { index, _ -> keys.subList(0, index).sum() }
    private val cnfVars: List<Int>
    val slideMargin: Int = this.lineSize - keys.sum() - this.keys.size + 1

    init {
        cnfVars = (0 until (this.slideMargin + 1) * this.keys.size)
            .mapTo(mutableListOf()) {
                ++varCount
            }
    }

    fun getCnfVar(keyIndex: Int, slideIndex: Int): Int? {
        val index = keyIndex * (slideMargin + 1) + slideIndex
        return cnfVars.getOrNull(index)
    }

    fun getPreKeysSum(keyIndex: Int): Int? = preKeysSumList.getOrNull(keyIndex)
}