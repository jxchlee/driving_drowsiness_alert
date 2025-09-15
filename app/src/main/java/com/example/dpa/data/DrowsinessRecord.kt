package com.example.dpa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 날짜별 경고 횟수 저장을 위한 Entity
@Entity(tableName = "warning_records")
data class WarningRecord(
    @PrimaryKey val date: String, // "YYYY-MM-DD" 형식
    val count: Int
)

// EAR 값 저장을 위한 Entity
@Entity(tableName = "ear_records")
data class EarRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val earValue: Float,
    val date: String // "YYYY-MM-DD" 형식, 조회를 위함
)
