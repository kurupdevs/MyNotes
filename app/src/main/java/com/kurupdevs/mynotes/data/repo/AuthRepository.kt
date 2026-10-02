package com.kurupdevs.mynotes.data.repo

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kurupdevs.mynotes.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AccountInfo(val uid: String, val isAnonymous: Boolean, val name: String?, val email: String?, val photoUrl: String?)

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun authState(): Flow<AccountInfo?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { a ->
            val u = a.currentUser
            trySend(
                u?.let {
                    AccountInfo(
                        uid = it.uid,
                        isAnonymous = it.isAnonymous,
                        name = it.displayName,
                        email = it.email,
                        photoUrl = it.photoUrl?.toString()
                    )
                }
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun ensureSignedIn(): String {
        val cur = auth.currentUser
        if (cur != null) return cur.uid
        auth.signInAnonymously().await()
        return auth.currentUser!!.uid
    }

    fun currentUid(): String? = auth.currentUser?.uid
    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous ?: true

    suspend fun linkGoogle(): Result<Unit> = runCatching {
        val serverClientId = context.getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val manager = CredentialManager.create(context)
        val result = try {
            manager.getCredential(context, request)
        } catch (e: GetCredentialCancellationException) {
            throw RuntimeException("cancelled")
        }
        val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
        val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
        val user = auth.currentUser ?: throw RuntimeException("no user")
        user.linkWithCredential(firebaseCred).await()
    }

    suspend fun unlinkGoogle(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw RuntimeException("no user")
        val googleProvider = user.providerData.firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            ?: throw RuntimeException("not linked")
        user.unlink(googleProvider.providerId).await()
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        auth.currentUser?.delete()?.await()
    }
}
