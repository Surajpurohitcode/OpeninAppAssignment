package com.surajpurohit.openinappassignment.ui.fragments.links

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.surajpurohit.openinappassignment.data.repository.DashboardRepository

class LinksViewModelProvider(private val application: Application):
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LinksViewModel::class.java)) {
            return LinksViewModel(
                dashboardRepository = DashboardRepository(
                ), application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}