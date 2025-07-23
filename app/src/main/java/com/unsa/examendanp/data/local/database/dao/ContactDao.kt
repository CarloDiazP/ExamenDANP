package com.unsa.examendanp.data.local.database.dao

import androidx.room.*
import com.unsa.examendanp.data.local.database.entities.ContactEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE isSynced = 0")
    suspend fun getUnsyncedContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts ORDER BY timestamp DESC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE timestamp >= :startDate ORDER BY timestamp DESC")
    fun getContactsAfterDate(startDate: Date): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM contacts WHERE timestamp >= :startDate")
    fun getContactCountAfterDate(startDate: Date): Flow<Int>

    @Query("UPDATE contacts SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markContactsAsSynced(ids: List<String>)

    @Query("DELETE FROM contacts WHERE timestamp < :cutoffDate")
    suspend fun deleteOldContacts(cutoffDate: Date)

    @Query("SELECT * FROM contacts WHERE encounteredUserId = :userId AND timestamp >= :startDate")
    suspend fun getContactsWithUser(userId: String, startDate: Date): List<ContactEntity>
}