package com.youtubeyar.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/* --------------------------------- Entities -------------------------------- */

/** یک نقطه از آمار کانال که کاربر ثبت می‌کند (ردیاب پیشرفت) */
@Entity(tableName = "progress_entries")
data class ProgressEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                 // yyyy-MM-dd
    val subscribers: Int,
    val watchHours: Float,
    val createdAt: Long = System.currentTimeMillis()
)

/** وظیفه چک‌لیست نقشه راه — phase از 0 تا 3 (فازها) یا -1 برای وظیفه سفارشی */
@Entity(tableName = "checklist_tasks")
data class ChecklistTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phase: Int,
    val orderIndex: Int,
    val title: String,
    val description: String,
    val isDone: Boolean = false,
    val isCustom: Boolean = false,
    val doneAt: Long? = null
)

/** تاریخچه تولیدات هوش مصنوعی */
@Entity(tableName = "ai_history")
data class AiHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolType: String,             // TITLE / DESCRIPTION / TAGS / SCRIPT / IDEAS
    val topic: String,
    val result: String,               // خروجی متنی
    val createdAt: Long = System.currentTimeMillis()
)

/* ----------------------------------- DAOs ---------------------------------- */

@Dao
interface ProgressDao {
    @Insert
    suspend fun insert(entry: ProgressEntry): Long

    @Query("SELECT * FROM progress_entries ORDER BY createdAt ASC")
    suspend fun all(): List<ProgressEntry>

    @Query("SELECT * FROM progress_entries ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ProgressEntry>

    @Query("SELECT * FROM progress_entries ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): ProgressEntry?

    @Delete
    suspend fun delete(entry: ProgressEntry)
}

@Dao
interface ChecklistDao {
    @Insert
    suspend fun insertAll(tasks: List<ChecklistTask>)

    @Insert
    suspend fun insert(task: ChecklistTask): Long

    @Query("SELECT * FROM checklist_tasks ORDER BY phase ASC, orderIndex ASC")
    fun observeAll(): Flow<List<ChecklistTask>>

    @Query("UPDATE checklist_tasks SET isDone = :done, doneAt = :doneAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, doneAt: Long?)

    @Query("DELETE FROM checklist_tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM checklist_tasks")
    suspend fun count(): Int
}

@Dao
interface AiHistoryDao {
    @Insert
    suspend fun insert(history: AiHistory)

    @Query("SELECT * FROM ai_history ORDER BY createdAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<AiHistory>>

    @Query("DELETE FROM ai_history")
    suspend fun clear()
}

/* --------------------------------- Database -------------------------------- */

@Database(
    entities = [ProgressEntry::class, ChecklistTask::class, AiHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun aiHistoryDao(): AiHistoryDao
}
