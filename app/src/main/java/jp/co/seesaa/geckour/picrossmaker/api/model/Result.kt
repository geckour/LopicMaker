package jp.co.seesaa.geckour.picrossmaker.api.model

data class Result<out T>(val message: T) {
    data class Data<out T>(
            val data: T
    ) {
        data class Problems(
                val problems: List<Problem>
        )
    }
}