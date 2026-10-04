package com.example.myapplicationtoday

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// --- Sync State Constants ---
object SyncState {
    const val SYNCED = 0
    const val PENDING_INSERT = 1
    const val PENDING_UPDATE = 2
    const val PENDING_DELETE = 3
}

// --- Room Entities ---
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val durationText: String,
    val startTime: String,
    val timeInMillis: Long,
    val category: String,
    val projectId: String?,
    val userId: String = "guest_local",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncState: Int = SyncState.PENDING_INSERT
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val color: Int,
    val goalMinutes: Int?,
    val totalMinutes: Int,
    val userId: String = "guest_local",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncState: Int = SyncState.PENDING_INSERT
)

@Entity(tableName = "user_tags")
data class UserTagEntity(
    @PrimaryKey val id: String,
    val tagName: String,
    val userId: String = "guest_local",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncState: Int = SyncState.PENDING_INSERT
)

// --- Room DAOs ---
@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE isDeleted = 0 ORDER BY timeInMillis DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isDeleted = 0 ORDER BY timeInMillis DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE syncState != 0")
    suspend fun getPendingSyncSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE sessions SET isDeleted = 1, syncState = :syncState, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun softDeleteSession(sessionId: String, syncState: Int = SyncState.PENDING_DELETE, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sessions SET userId = :newUserId, syncState = :syncState, updatedAt = :updatedAt WHERE userId = 'guest_local'")
    suspend fun migrateGuestSessionsToAccount(newUserId: String, syncState: Int = SyncState.PENDING_INSERT, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isDeleted = 0")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isDeleted = 0")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE syncState != 0")
    suspend fun getPendingSyncProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("UPDATE projects SET isDeleted = 1, syncState = :syncState, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun softDeleteProject(projectId: String, syncState: Int = SyncState.PENDING_DELETE, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET userId = :newUserId, syncState = :syncState, updatedAt = :updatedAt WHERE userId = 'guest_local'")
    suspend fun migrateGuestProjectsToAccount(newUserId: String, syncState: Int = SyncState.PENDING_INSERT, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM projects")
    suspend fun clearAll()
}

@Dao
interface UserTagDao {
    @Query("SELECT * FROM user_tags WHERE isDeleted = 0")
    suspend fun getAllTags(): List<UserTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: UserTagEntity)

    @Query("UPDATE user_tags SET isDeleted = 1, syncState = :syncState, updatedAt = :updatedAt WHERE tagName = :tagName")
    suspend fun softDeleteTag(tagName: String, syncState: Int = SyncState.PENDING_DELETE, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_tags")
    suspend fun clearAll()
}

// --- App Room Database ---
@Database(
    entities = [SessionEntity::class, ProjectEntity::class, UserTagEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun projectDao(): ProjectDao
    abstract fun userTagDao(): UserTagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "metanoia_room_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Dao replacement class maintaining backwards compatibility for existing direct calls while supporting Room
class SessionDaoReplacement(private val context: Context) {
    init {
        SessionRepository.init(context)
    }

    private val _sessionsFlow = MutableStateFlow<List<Session>>(emptyList())
    val allSessionsFlow: Flow<List<Session>> = _sessionsFlow

    init {
        refresh()
    }

    fun refresh() {
        _sessionsFlow.value = SessionRepository.getAllSessions(context)
    }

    fun getAllSessions(): List<Session> {
        return SessionRepository.getAllSessions(context)
    }

    fun insertSession(session: Session) {
        SessionRepository.addSession(context, session)
        refresh()
    }

    fun updateSession(session: Session) {
        SessionRepository.updateSession(context, session)
        refresh()
    }

    fun deleteSession(sessionId: String) {
        SessionRepository.deleteSession(context, sessionId)
        refresh()
    }
}
