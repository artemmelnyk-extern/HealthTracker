package com.genegebra.healthtracker.data.repository

import com.genegebra.healthtracker.domain.model.User
import com.genegebra.healthtracker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionPersistenceTest {

    private val userFlow = MutableStateFlow<User?>(null)

    private val fakeRepo: AuthRepository = object : AuthRepository {
        override val currentUser: Flow<User?> = userFlow
        override suspend fun login(email: String, password: String) =
            Result.success(User(uid = "u", email = email, isEmailVerified = true))
        override suspend fun register(email: String, password: String, recaptchaToken: String) =
            Result.success(Unit)
        override suspend fun logout() { userFlow.value = null }
        override suspend fun sendVerificationEmail() = Result.success(Unit)
        override suspend fun reloadUser() =
            Result.success(userFlow.value ?: error("No user"))
    }

    // --- Flow emission ---

    @Test
    fun `no active session emits null`() = runTest {
        assertNull(fakeRepo.currentUser.first())
    }

    @Test
    fun `active verified session emits non-null user`() = runTest {
        val user = User(uid = "uid1", email = "test@example.com", isEmailVerified = true)
        userFlow.value = user
        val emitted = fakeRepo.currentUser.first()
        assertNotNull(emitted)
        assertTrue(emitted!!.isEmailVerified)
    }

    @Test
    fun `unverified user is emitted but not fully authenticated`() = runTest {
        val unverified = User(uid = "uid1", email = "test@example.com", isEmailVerified = false)
        userFlow.value = unverified
        val emitted = fakeRepo.currentUser.first()
        assertNotNull(emitted)
        assertFalse(emitted!!.isEmailVerified)
    }

    @Test
    fun `logout clears the session`() = runTest {
        userFlow.value = User(uid = "uid1", email = "test@example.com", isEmailVerified = true)
        fakeRepo.logout()
        assertNull(fakeRepo.currentUser.first())
    }

    // --- Navigation decision logic (mirrors LaunchedEffect in NavGraph) ---

    @Test
    fun `verified user on auth screen triggers navigation to main`() {
        val user = User(uid = "uid1", email = "test@example.com", isEmailVerified = true)
        assertTrue(shouldGoToMain(user, currentDest = "auth"))
    }

    @Test
    fun `unverified user on auth screen does not trigger navigation to main`() {
        val user = User(uid = "uid1", email = "test@example.com", isEmailVerified = false)
        assertFalse(shouldGoToMain(user, currentDest = "auth"))
    }

    @Test
    fun `verified user already on main does not re-trigger navigation`() {
        val user = User(uid = "uid1", email = "test@example.com", isEmailVerified = true)
        assertFalse(shouldGoToMain(user, currentDest = "main"))
    }

    @Test
    fun `null user on main triggers redirect to auth`() {
        assertTrue(shouldGoToAuth(user = null, currentDest = "main"))
    }

    @Test
    fun `null user already on auth does not trigger redirect`() {
        assertFalse(shouldGoToAuth(user = null, currentDest = "auth"))
    }

    @Test
    fun `verified user on main does not trigger redirect to auth`() {
        val user = User(uid = "uid1", email = "test@example.com", isEmailVerified = true)
        assertFalse(shouldGoToAuth(user, currentDest = "main"))
    }

    // Pure functions that mirror the LaunchedEffect conditions in NavGraph.kt
    private fun shouldGoToMain(user: User?, currentDest: String?) =
        user != null && user.isEmailVerified && currentDest == "auth"

    private fun shouldGoToAuth(user: User?, currentDest: String?) =
        user == null && currentDest == "main"
}
