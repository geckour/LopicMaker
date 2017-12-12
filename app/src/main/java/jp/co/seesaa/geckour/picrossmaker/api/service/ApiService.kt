package jp.co.seesaa.geckour.picrossmaker.api.service

import io.reactivex.Single
import jp.co.seesaa.geckour.picrossmaker.api.model.Problem
import jp.co.seesaa.geckour.picrossmaker.api.model.Result
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/v1/post")
    fun registerProblem(
            @Body
            problem: Problem
    ): Single<Result<String>>

    @GET("api/v1/search")
    fun search(
            @Query("title")
            title: String?,

            @Query("genre")
            genre: String?
    ): Single<Result<Result.Data<Result.Data.Problems>>>
}