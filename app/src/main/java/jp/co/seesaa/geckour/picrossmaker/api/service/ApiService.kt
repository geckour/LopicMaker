package jp.co.seesaa.geckour.picrossmaker.api.service

import io.reactivex.Single
import jp.co.seesaa.geckour.picrossmaker.api.model.Problem
import jp.co.seesaa.geckour.picrossmaker.api.model.Result
import jp.co.seesaa.geckour.picrossmaker.api.model.SearchQuery
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/problems")
    fun registerProblem(
            @Body
            problem: Problem
    ): Single<Response<Result<Result.Data<String>>>>

    @FormUrlEncoded
    @POST("api/v1/search/problems")
    fun search(
            @Field("title")
            title: String?,

            @Field("tags")
            tags: List<String>?
    ): Single<Response<Result<Result.Data<Result.Data.Problems>>>>
}