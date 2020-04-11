package com.wolk.android.ct

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface API {
    // post DailyTracingKey
    @POST("/diagnosis")
    fun postDiagnosisKeys(@Body report : List<DailyTracingKey>): Call<String>

    // get Array<Report>, based on timestamp
    @GET("/diagnosis?since={timestamp}")
    fun getDiagnosisKeys(@Path("timestamp") timestamp: UInt): Call<List<DailyTracingKey>>

    companion object {
        fun create(): API {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://api.wolk.com/")
                .build()
            return retrofit.create(API::class.java)
        }
    }
}

