package dev.jurczyk.renoplan

import android.app.Application
import dev.jurczyk.renoplan.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class RenoPlanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@RenoPlanApplication)
        }
    }
}
