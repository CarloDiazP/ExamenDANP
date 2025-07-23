package com.unsa.examendanp.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.unsa.examendanp.data.remote.dto.ContactDto
import com.unsa.examendanp.utils.Constants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor() {

    private val firestore: FirebaseFirestore = Firebase.firestore

    suspend fun uploadContacts(userId: String, contacts: List<ContactDto>): Result<Unit> {
        return try {
            val batch = firestore.batch()

            contacts.forEach { contact ->
                val docRef = firestore
                    .collection(Constants.CONTACTS_COLLECTION)
                    .document(userId)
                    .collection("encounters")
                    .document(contact.id)

                batch.set(docRef, contact)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(userId: String, fcmToken: String): Result<Unit> {
        return try {
            val userDoc = hashMapOf(
                "userId" to userId,
                "fcmToken" to fcmToken,
                "registeredAt" to System.currentTimeMillis(),
                "isInfected" to false
            )

            firestore
                .collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(userDoc)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(userId: String, fcmToken: String): Result<Unit> {
        return try {
            firestore
                .collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update("fcmToken", fcmToken)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkInfectionStatus(userId: String): Result<Boolean> {
        return try {
            val document = firestore
                .collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            val isInfected = document.getBoolean("isInfected") ?: false
            Result.success(isInfected)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}