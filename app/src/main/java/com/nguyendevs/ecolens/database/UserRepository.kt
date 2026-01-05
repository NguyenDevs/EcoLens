package com.nguyendevs.ecolens.database

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://ecolens-658ae-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val usersRef = database.getReference("users")

    suspend fun registerUser(email: String, password: String, username: String): Boolean {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return false

            val newUser = User(
                username = username,
                email = email,
                language = "vi",
                darkMode = false
            )
            
            usersRef.child(firebaseUser.uid).setValue(newUser).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun loginUser(email: String, password: String): FirebaseUser? {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun signInWithCredential(credential: AuthCredential): FirebaseUser? {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                val snapshot = usersRef.child(firebaseUser.uid).get().await()
                if (!snapshot.exists()) {
                    val newUser = User(
                        username = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                        email = firebaseUser.email ?: "",
                        language = "vi",
                        darkMode = false
                    )
                    usersRef.child(firebaseUser.uid).setValue(newUser).await()
                }
            }
            firebaseUser
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCurrentUserDetails(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = usersRef.child(uid).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUser(user: User) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).setValue(user).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateDarkMode(isDarkMode: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).child("darkMode").setValue(isDarkMode).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun logout() {
        auth.signOut()
    }
}