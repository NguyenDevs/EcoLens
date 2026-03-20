package com.nguyendevs.ecolens.database

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.models.User
import kotlinx.coroutines.tasks.await

/** Quản lý xác thực và thông tin người dùng qua Firebase Auth và Realtime Database. */
class UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val usersRef = database.getReference("users")

    /** Đăng ký tài khoản mới với email, mật khẩu và username. */
    suspend fun registerUser(email: String, password: String, username: String): Boolean {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return false

            val newUser =
                    User(
                            username = username,
                            email = email,
                            language = "vi",
                            darkMode = false,
                            iucnMode = true,
                            taxoMode = false
                    )

            usersRef.child(firebaseUser.uid).setValue(newUser).await()

            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(username).build()
            firebaseUser.updateProfile(profileUpdates).await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Đăng nhập bằng email và mật khẩu. */
    suspend fun loginUser(email: String, password: String): FirebaseUser? {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Đăng nhập bằng credential (Google, v.v.), tự tạo profile nếu lần đầu. */
    suspend fun signInWithCredential(credential: AuthCredential): FirebaseUser? {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val snapshot = usersRef.child(firebaseUser.uid).get().await()
                if (!snapshot.exists()) {
                    val newUser =
                            User(
                                    username = firebaseUser.displayName
                                                    ?: firebaseUser.email?.substringBefore("@")
                                                            ?: "User",
                                    email = firebaseUser.email ?: "",
                                    language = "vi",
                                    darkMode = false,
                                    iucnMode = true,
                                    taxoMode = false
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

    /** Xác thực lại người dùng bằng credential. */
    fun reauthenticateUser(credential: AuthCredential, onResult: (Boolean) -> Unit) {
        val user = auth.currentUser
        user?.reauthenticate(credential)?.addOnCompleteListener { task ->
            onResult(task.isSuccessful)
        }
    }

    /** Kiểm tra người dùng hiện tại đã đăng nhập chưa. */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /** Đăng xuất người dùng hiện tại. */
    fun logout() {
        auth.signOut()
    }

    /** Lấy thông tin chi tiết người dùng hiện tại từ Database. */
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

    /** Cập nhật toàn bộ thông tin người dùng. */
    suspend fun updateUser(user: User) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).setValue(user).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Cập nhật tên hiển thị của người dùng. */
    suspend fun updateUsername(newUsername: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).child("username").setValue(newUsername).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Cập nhật chế độ dark mode. */
    suspend fun updateDarkMode(isDarkMode: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).child("darkMode").setValue(isDarkMode).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Cập nhật ngôn ngữ của người dùng. */
    suspend fun updateLanguage(language: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).child("language").setValue(language).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Cập nhật chế độ IUCN. */
    suspend fun updateIucnMode(isEnabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).child("iucnMode").setValue(isEnabled).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Cập nhật chế độ dịch phân loại học. */
    suspend fun updateTaxoMode(isEnabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).child("taxoMode").setValue(isEnabled).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
