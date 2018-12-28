package jp.co.seesaa.geckour.picrossmaker.api

import io.reactivex.Single
import jp.co.seesaa.geckour.picrossmaker.App.Companion.gson
import jp.co.seesaa.geckour.picrossmaker.api.model.Problem
import jp.co.seesaa.geckour.picrossmaker.api.model.Result
import jp.co.seesaa.geckour.picrossmaker.api.service.ApiService
import jp.co.seesaa.geckour.picrossmaker.util.OkHttpProvider
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {

    companion object {
        private const val baseUrl = "172.16.21.29:12443/pm"
    }

    private val service = Retrofit.Builder().client(OkHttpProvider.client)
            .baseUrl("http://$baseUrl/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)

    fun registerProblem(problem: Problem): Single<Response<Result<Result.Data<String>>>> = service.registerProblem(problem)

    fun search(title: String?, tags: List<String>?): Single<Response<Result<Result.Data<Result.Data.Problems>>>> = service.search(title, tags)
}