package com.nguyendevs.ecolens.database

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.model.User
import kotlinx.coroutines.tasks.await

/**
 * Repository quản lý xác thực và thông tin người dùng
 * Tích hợp Firebase Authentication và Realtime Database
 */
class UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val usersRef = database.getReference("users")

    // ==================== AUTHENTICATION - REGISTER ====================

    /**
     * Đăng ký người dùng mới với email và password
     * Tự động tạo profile trong Realtime Database
     * @return true nếu đăng ký thành công, false nếu thất bại
     */
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

    // ==================== AUTHENTICATION - LOGIN ====================

    /**
     * Đăng nhập với email và password
     * @return FirebaseUser nếu thành công, null nếu thất bại
     */
    suspend fun loginUser(email: String, password: String): FirebaseUser? {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Đăng nhập với credential (Google, Facebook, etc.)
     * Tự động tạo profile nếu là lần đầu đăng nhập
     * @return FirebaseUser nếu thành công, null nếu thất bại
     */
    suspend fun signInWithCredential(credential: AuthCredential): FirebaseUser? {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val snapshot = usersRef.child(firebaseUser.uid).get().await()
                if (!snapshot.exists()) {
                    val newUser = User(
                        username = firebaseUser.displayName
                            ?: firebaseUser.email?.substringBefore("@")
                            ?: "User",
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

    // ==================== AUTHENTICATION - LOGOUT ====================

    /**
     * Kiểm tra xem người dùng đã đăng nhập hay chưa
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Đăng xuất người dùng hiện tại
     */
    fun logout() {
        auth.signOut()
    }

    // ==================== USER DATA - READ ====================

    /**
     * Lấy thông tin chi tiết của người dùng hiện tại từ Database
     * @return User object nếu tìm thấy, null nếu không
     */
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

    // ==================== USER DATA - UPDATE ====================

    /**
     * Cập nhật toàn bộ thông tin người dùng
     */
    suspend fun updateUser(user: User) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).setValue(user).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cập nhật chế độ dark mode của người dùng
     */
    suspend fun updateDarkMode(isDarkMode: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersRef.child(uid).child("darkMode").setValue(isDarkMode).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}