package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A named point-in-time snapshot of a project - the project time machine.
 *
 * Snapshots carry the full serialised project, so restoring one rolls the
 * timeline back exactly, independent of the 200-step undo window which only
 * covers the current session.
 */
@Entity(tableName = "project_versions")
data class ProjectVersionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "projectId")
    val projectId: String,
    @ColumnInfo(name = "label")
    val label: String,
    /** Serialised [com.example.model.VideoProject] snapshot. */
    @ColumnInfo(name = "payload")
    val payload: String,
    /** Total clips at snapshot time, for the list subtitle. */
    @ColumnInfo(name = "clipCount")
    val clipCount: Int,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long
)

@Dao
interface ProjectVersionDao {

    @Insert
    suspend fun insert(version: ProjectVersionEntity)

    @Query("SELECT * FROM project_versions WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 50")
    fun versionsFor(projectId: String): Flow<List<ProjectVersionEntity>>

    @Query("SELECT * FROM project_versions WHERE id = :id")
    suspend fun byId(id: String): ProjectVersionEntity?

    @Query("DELETE FROM project_versions WHERE id = :id")
    suspend fun delete(id: String)

    /** Keep the newest [keep] versions per project; prune the rest. */
    @Query(
        """
        DELETE FROM project_versions WHERE projectId = :projectId AND id NOT IN (
            SELECT id FROM project_versions WHERE projectId = :projectId
            ORDER BY createdAt DESC LIMIT :keep
        )
        """
    )
    suspend fun prune(projectId: String, keep: Int)
}
