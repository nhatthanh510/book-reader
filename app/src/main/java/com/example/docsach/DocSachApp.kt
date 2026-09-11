package com.example.docsach

import android.app.Application
import com.example.docsach.di.AppContainer

class DocSachApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
