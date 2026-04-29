package com.forma.app

import android.app.Application
import com.forma.app.data.backup.AppBackupService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FormaApp : Application() {

    @Inject
    lateinit var backup: AppBackupService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 1) Пытаемся восстановить данные из бэкапа, если БД пустая
        // 2) Через 5 секунд после старта сохраняем актуальный снимок
        // 3) Дальше сохраняем каждые 60 секунд пока приложение живёт
        scope.launch {
            try {
                val restored = backup.restoreIfNeeded()
                android.util.Log.d("Forma.Backup", "Initial restore: $restored")

                delay(5_000)
                while (true) {
                    backup.save()
                    delay(60_000)
                }
            } catch (t: Throwable) {
                android.util.Log.e("Forma.Backup", "Backup loop crashed", t)
            }
        }
    }
}
