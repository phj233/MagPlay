package top.phj233.magplay

import android.app.Application
import com.tencent.mmkv.MMKV
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import top.phj233.magplay.di.appModule
import top.phj233.magplay.repository.DBUtil

class MagPlayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化 MMKV
        MMKV.initialize(this)
        // 初始化 Koin
        startKoin {
            androidContext(this@MagPlayApplication)
            modules(appModule)
        }
        // 初始化数据库
        DBUtil.initializeDB(applicationContext)
    }
}