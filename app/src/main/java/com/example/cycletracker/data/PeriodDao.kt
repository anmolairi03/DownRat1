package com.example.cycletracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM period_records ORDER BY startDate DESC")
    fun getAllRecords(): Flow<List<PeriodRecord>>

    @Query("SELECT * FROM period_records ORDER BY startDate DESC")
    suspend fun getAllRecordsSync(): List<PeriodRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PeriodRecord): Long

    @Update
    suspend fun updateRecord(record: PeriodRecord)

    @Delete
    suspend fun deleteRecord(record: PeriodRecord)

    @Query("DELETE FROM period_records")
    suspend fun deleteAllRecords()
}
