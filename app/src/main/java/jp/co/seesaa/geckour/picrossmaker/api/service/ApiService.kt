package jp.co.seesaa.geckour.picrossmaker.api.service

import io.reactivex.Single
import jp.co.seesaa.geckour.picrossmaker.api.model.Problem
import jp.co.seesaa.geckour.picrossmaker.api.model.Result
import jp.co.seesaa.geckour.picrossmaker.api.model.SearchQuery
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/problems")
    fun registerProblem(
            @Body
            problem: Problem
    ): Single<Result<String>>

    @POST("api/v1/search/problems")
    fun search(
            @Body
            query: SearchQuery
    ): Single<Result<Result.Data<Result.Data.Problems>>>
}