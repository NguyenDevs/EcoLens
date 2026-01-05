package com.nguyendevs.ecolens.database

import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.model.User
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class UserRepository {
    private val database = FirebaseDatabase.getInstance("https://ecolens-658ae-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val usersRef = database.getReference("users")

    suspend fun registerUser(user: User): Boolean {
        return try {
            val username = user.username
            val snapshot = usersRef.child(username).get().await()
            if (snapshot.exists()) {
                return false // User already exists
            }
            usersRef.child(username).setValue(user).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loginUser(username: String, passwordHash: String): User? {
        return try {
            val snapshot = usersRef.child(username).get().await()
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java)
                if (user != null && user.passwordHash == passwordHash) {
                    return user
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getUser(username: String): User? {
        return try {
            val snapshot = usersRef.child(username).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUser(user: User) {
        try {
            usersRef.child(user.username).setValue(user).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}