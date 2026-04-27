package com.forma.app.presentation.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.backup.BackupRepository
import com.forma.app.domain.backup.RestoreSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class BackupStatus {
    data object Idle : BackupStatus()
    data object Working : BackupStatus()
    data class ExportSuccess(val fileName: String) : BackupStatus()
    data class ImportSuccess(val summary: RestoreSummary) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

data class SettingsUi(
    val status: BackupStatus = BackupStatus.Idle
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    app: Application,
    private val backupRepo: BackupRepository
) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(SettingsUi())
    val ui: StateFlow<SettingsUi> = _ui.asStateFlow()

    /** Предложить юзеру имя файла по умолчанию для экспорта. */
    fun suggestExportFileName(): String {
        val ts = java.text.SimpleDateFormat("yyyy-MM-dd_HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "forma-backup-$ts.json"
    }

    fun export(targetUri: Uri) {
        viewModelScope.launch {
            _ui.update { it.copy(status = BackupStatus.Working) }
            try {
                val snapshot = backupRepo.createSnapshot()
                val jsonText = backupRepo.encodeToJson(snapshot)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openOutputStream(targetUri)
                        ?.use { it.write(jsonText.toByteArray(Charsets.UTF_8)) }
                        ?: error("Не удалось открыть файл для записи")
                }
                val displayName = uriDisplayName(targetUri)
                _ui.update { it.copy(status = BackupStatus.ExportSuccess(displayName)) }
                android.util.Log.d("Forma.Backup", "Exported to $displayName")
            } catch (t: Throwable) {
                android.util.Log.e("Forma.Backup", "Export failed", t)
                _ui.update {
                    it.copy(status = BackupStatus.Error("Экспорт не удался: ${t.message}"))
                }
            }
        }
    }

    fun import(sourceUri: Uri) {
        viewModelScope.launch {
            _ui.update { it.copy(status = BackupStatus.Working) }
            try {
                val jsonText = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(sourceUri)
                        ?.use { it.bufferedReader().readText() }
                        ?: error("Не удалось открыть файл")
                }
                val snapshot = backupRepo.decodeFromJson(jsonText)
                val summary = backupRepo.restoreFromSnapshot(snapshot)
                _ui.update { it.copy(status = BackupStatus.ImportSuccess(summary)) }
                android.util.Log.d("Forma.Backup", "Imported: $summary")
            } catch (t: Throwable) {
                android.util.Log.e("Forma.Backup", "Import failed", t)
                _ui.update {
                    it.copy(status = BackupStatus.Error("Импорт не удался: ${t.message}"))
                }
            }
        }
    }

    fun resetStatus() {
        _ui.update { it.copy(status = BackupStatus.Idle) }
    }

    private fun uriDisplayName(uri: Uri): String =
        uri.lastPathSegment?.substringAfterLast('/') ?: "файл"
}
