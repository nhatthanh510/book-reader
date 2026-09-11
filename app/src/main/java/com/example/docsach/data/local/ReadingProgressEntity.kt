package com.example.docsach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Where the user left off, so the shelf can show "Đang đọc" and the reader can restore position. */
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val lastReadAt: Long,
    val scrollOffset: Int = 0,
    val fontScalePercent: Int = 100,
)
