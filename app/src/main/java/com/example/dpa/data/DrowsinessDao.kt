package com.example.dpa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrowsinessDao {
    // 경고 기록 추가 또는 업데이트
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWarning(warningRecord: WarningRecord)

    // 특정 날짜의 경고 기록 가져오기
    @Query("SELECT * FROM warning_records WHERE date = :date LIMIT 1")
    suspend fun getWarningRecordByDate(date: String): WarningRecord?

    // 모든 경고 기록 가져오기 (최신순)
    @Query("SELECT * FROM warning_records ORDER BY date DESC")
    fun getAllWarningRecords(): Flow<List<WarningRecord>>

    // EAR 기록 추가
    @Insert
    suspend fun insertEarRecord(earRecord: EarRecord)

    // 특정 날짜의 EAR 기록 가져오기
    @Query("SELECT * FROM ear_records WHERE date = :date ORDER BY timestamp ASC")
    fun getEarRecordsByDate(date: String): Flow<List<EarRecord>>
}