package com.kurupdevs.mynotes

import android.app.Application
import com.kurupdevs.mynotes.di.AppContainer

class NotesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
