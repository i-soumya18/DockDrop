package `in`.soumyaranjansahoo.dockdrop

import android.app.Application
import `in`.soumyaranjansahoo.dockdrop.core.di.AppContainer

class DockDropApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
