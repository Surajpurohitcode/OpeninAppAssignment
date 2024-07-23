package com.surajpurohit.openinappassignment

import android.app.Application
import android.content.Context

class OpeninApplication: Application(){

    companion object{
        var appContext: Context? = null
    }
}