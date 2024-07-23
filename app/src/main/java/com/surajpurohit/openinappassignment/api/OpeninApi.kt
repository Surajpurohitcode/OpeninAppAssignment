package com.surajpurohit.openinappassignment.api

import com.surajpurohit.openinappassignment.data.model.DashboardResponse
import retrofit2.Response
import retrofit2.http.GET

interface OpeninApi {
    @GET("api/v1/dashboardNew")
    suspend fun dashboardApi(): Response<DashboardResponse>

}