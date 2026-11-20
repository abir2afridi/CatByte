package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One queued export. The full project payload travels with the job, so a
 * queue entry stays exportable even if the project is later edited or
 * deleted - exactly what retry and history need.
 */
@Entity(tableName = "export_jobs")
data class ExportJobEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    /** Serialised [com.example.model.VideoProject]. */
    @ColumnInfo(name = "payload")
    val payload: String,
    @ColumnInfo(name = "resolution")
    val resolution: String,
    @ColumnInfo(name = "fps")
    val fps: Int,
    @ColumnInfo(name = "watermark")
    val watermark: Boolean,
    @ColumnInfo(name = "bitrateMbps")
    val bitrateMbps: Int,
    @ColumnInfo(name = "audioBoost")
    val audioBoost: Float,
    /** QUEUED / RUNNING / DONE / FAILED / CANCELLED. */
    @ColumnInfo(name = "state")
    val state: String,
    @ColumnInfo(name = "progress")
    val progress: Float,
    @ColumnInfo(name = "outputPath")
    val outputPath: String?,
    @ColumnInfo(name = "error")
    val error: String?,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long
) {
    companion object {
        const val STATE_QUEUED = "QUEUED"
        const val STATE_RUNNING = "RUNNING"
        const val STATE_DONE = "DONE"
        const val STATE_FAILED = "FAILED"
        const val STATE_CANCELLED = "CANCELLED"
    }
}

@Dao
interface ExportJobDao {

    @Insert
    suspend fun insert(job: ExportJobEntity)

    @Query("UPDATE export_jobs SET state = :state, progress = :progress, outputPath = :outputPath, error = :error WHERE id = :id")
    suspend fun updateResult(id: String, state: String, progress: Float, outputPath: String?, error: String?)

    @Query("UPDATE export_jobs SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float)

    /** The oldest queued job - the queue is strict FIFO. */
    @Query("SELECT * FROM export_jobs WHERE state = 'QUEUED' ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextQueued(): ExportJobEntity?

    @Query("SELECT COUNT(*) FROM export_jobs WHERE state = 'QUEUED'")
    suspend fun queuedCount(): Int

    @Query("SELECT * FROM export_jobs ORDER BY createdAt DESC LIMIT 100")
    fun history(): Flow<List<ExportJobEntity>>

    @Query("SELECT * FROM export_jobs WHERE id = :id")
    suspend fun byId(id: String): ExportJobEntity?

    /** Retry = re-queue an existing job verbatim. */
    @Query("UPDATE export_jobs SET state = 'QUEUED', progress = 0, outputPath = NULL, error = NULL WHERE id = :id")
    suspend fun requeue(id: String)

    @Query("UPDATE export_jobs SET state = :state WHERE id = :id")
    suspend fun setState(id: String, state: String)

    @Query("DELETE FROM export_jobs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM export_jobs WHERE state IN ('DONE', 'FAILED', 'CANCELLED')")
    suspend fun clearFinished()
}
