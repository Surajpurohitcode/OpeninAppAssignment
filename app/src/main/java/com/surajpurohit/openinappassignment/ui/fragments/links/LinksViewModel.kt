package com.surajpurohit.openinappassignment.ui.fragments.links

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.surajpurohit.openinappassignment.OpeninApplication
import com.surajpurohit.openinappassignment.data.model.DashboardResponse
import com.surajpurohit.openinappassignment.data.repository.DashboardRepository
import com.surajpurohit.openinappassignment.util.MutableAppropriateLiveData
import com.surajpurohit.openinappassignment.util.Resource
import com.surajpurohit.openinappassignment.util.hasInternetConnection
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class LinksViewModel(private val dashboardRepository: DashboardRepository, app: Application) :
    AndroidViewModel(app) {

    private val _dashboardResponse = MutableAppropriateLiveData<Resource<DashboardResponse>>()
    val dashboardResponseResult: LiveData<Resource<DashboardResponse>> = _dashboardResponse

    fun getDashboardApiData() {
        viewModelScope.launch {
            safeGetDashboardResponse()
        }
    }

    private suspend fun safeGetDashboardResponse() {
        _dashboardResponse.postValue(Resource.Loading())

        try {
            val connectivityManager = getApplication<OpeninApplication>().getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager
            if (hasInternetConnection(connectivityManager)) {
                val response = dashboardRepository.getDashboardApiResponse()
                val successful = response.isSuccessful

                if (successful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        _dashboardResponse.postValue(Resource.Success(responseBody))
                    } else {
                        _dashboardResponse.postValue(Resource.Error("Empty Response Body"))
                    }
                } else {
                    _dashboardResponse.postValue(Resource.Error(response.message() ?: "API Error"))
                }
            } else {
                _dashboardResponse.postValue(Resource.Error("No internet connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> _dashboardResponse.postValue(Resource.Error("Network Failure"))
                else -> {
                    // Log the exception for debugging
                    Log.e("LinksViewModel", "Error fetching data:", t)
                    _dashboardResponse.postValue(Resource.Error("Unknown Error"))
                }
            }
        }
    }

    private fun hasInternetConnection(connectivityManager: ConnectivityManager): Boolean {
        // Implement a reliable internet check (e.g., using NetworkCallback or pinging a known host)
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
