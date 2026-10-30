package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A stored project. The full editing model is kept as JSON so the schema stays
 * stable while the model evolves, while the most important columns are stored
 * separately for fast queries.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "payload")
    val payload: String,

    @ColumnInfo(name = "aspect_ratio")
    val aspectRatio: String,

    @ColumnInfo(name = "resolution")
    val resolution: String,

    @ColumnInfo(name = "fps")
    val fps: Int,

    @ColumnInfo(name = "paper_style")
    val paperStyle: String,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long
)
