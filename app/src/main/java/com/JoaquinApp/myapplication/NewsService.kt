package com.JoaquinApp.myapplication

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsService {
    @GET("events")
    fun getNaturalDisasters(
        @Query("status") status: String = "open",
        @Query("limit") limit: Int = 20
    ): Call<EonetResponse>
}
