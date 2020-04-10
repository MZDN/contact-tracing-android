package com.wolk.android.tcn

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TCNApi {
    // post Report, which contains info needed for matching
    @POST("/report")
    fun postReport(@Body report : List<Report>): Call<String>

    // get Array<Report>, based on timestamp
    @POST("/query/{timestamp}")
    fun query(@Body query : ByteArray, @Path("timestamp") timestamp: Int): Call<Array<Report>>

    companion object {
        fun create(): TCNApi {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://tcn.wolk.com/")
                .build()
            return retrofit.create(TCNApi::class.java)
        }
    }
}

