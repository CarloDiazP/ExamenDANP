package com.unsa.examendanp.data.repository

import com.unsa.examendanp.data.local.database.dao.ContactDao
import com.unsa.examendanp.data.local.database.entities.ContactEntity
import com.unsa.examendanp.data.remote.dto.ContactDto
import com.unsa.examendanp.data.remote.firebase.FirebaseRepository
import com.unsa.examendanp.domain.model.Contact
import com.unsa.examendanp.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: ContactDao,
    private val firebaseRepository: FirebaseRepository
) {

    fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getRecentContactCount(): Flow<Int> {
        val cutoffDate = Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)) // Last 24 hours
        return contactDao.getContactCountAfterDate(cutoffDate)
    }

    suspend fun saveContact(contact: ContactEntity) {
        contactDao.insertContact(contact)
    }

    suspend fun syncContacts(userId: String): Result<Unit> {
        return try {
            val unsyncedContacts = contactDao.getUnsyncedContacts()

            if (unsyncedContacts.isNotEmpty()) {
                val contactDtos = unsyncedContacts.map { it.toDto() }

                val result = firebaseRepository.uploadContacts(userId, contactDtos)

                if (result.isSuccess) {
                    contactDao.markContactsAsSynced(unsyncedContacts.map { it.id })
                }

                result
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cleanupOldData() {
        val cutoffDate = Date(System.currentTimeMillis() - (Constants.DATA_RETENTION_DAYS * 24 * 60 * 60 * 1000L))
        contactDao.deleteOldContacts(cutoffDate)
    }

    suspend fun registerUser(userId: String, fcmToken: String): Result<Unit> {
        return firebaseRepository.registerUser(userId, fcmToken)
    }

    suspend fun updateFcmToken(userId: String, fcmToken: String): Result<Unit> {
        return firebaseRepository.updateFcmToken(userId, fcmToken)
    }

    suspend fun checkInfectionStatus(userId: String): Result<Boolean> {
        return firebaseRepository.checkInfectionStatus(userId)
    }
}

// Extension functions
private fun ContactEntity.toDomainModel(): Contact {
    return Contact(
        id = id,
        userId = userId,
        encounteredUserId = encounteredUserId,
        timestamp = timestamp,
        duration = duration,
        rssi = rssi,
        distance = distance,
        isSynced = isSynced
    )
}

private fun ContactEntity.toDto(): ContactDto {
    return ContactDto(
        id = id,
        userId = userId,
        encounteredUserId = encounteredUserId,
        timestamp = timestamp.time,
        duration = duration,
        rssi = rssi,
        distance = distance
    )
}