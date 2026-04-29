package com.forma.app.domain.backup

interface BackupRepository {
    /**
     * Собирает полный снимок текущего состояния.
     */
    suspend fun createSnapshot(): BackupSnapshot

    /**
     * Восстанавливает из снимка. Полностью перезаписывает текущие данные.
     * Возвращает кол-во импортированных сущностей в виде понятной строки для UI.
     */
    suspend fun restoreFromSnapshot(snapshot: BackupSnapshot): RestoreSummary

    /**
     * Сериализует снапшот в JSON-строку.
     */
    fun encodeToJson(snapshot: BackupSnapshot): String

    /**
     * Парсит JSON в снапшот. Бросает исключение если формат невалидный.
     */
    fun decodeFromJson(json: String): BackupSnapshot
}

data class RestoreSummary(
    val sessionsImported: Int,
    val reviewsImported: Int,
    val overridesImported: Int,
    val programLoaded: Boolean,
    val profileLoaded: Boolean
)
