package com.surajpurohit.openinappassignment.data.repository

import com.surajpurohit.openinappassignment.api.RetrofitInstance

class DashboardRepository {

    suspend fun getDashboardApiResponse() =
        RetrofitInstance.api.dashboardApi()
}