package com.fintrack.app.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.fintrack.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class Account(val uid: String, val label: String)

class AuthException(message: String) : Exception(message)

/** Google and email/password sign-in through Firebase Authentication. */
class AuthManager(private val context: Context) {
    val configured: Boolean = BuildConfig.FIREBASE_CONFIGURED

    private val auth: FirebaseAuth? by lazy { if (configured) FirebaseAuth.getInstance() else null }

    fun current(): Account? = auth?.currentUser?.toAccount()

    suspend fun signInWithEmail(email: String, password: String): Account = firebase {
        requireAuth().signInWithEmailAndPassword(email.trim(), password).await().user!!.toAccount()
    }

    suspend fun createAccount(email: String, password: String): Account = firebase {
        val user = requireAuth().createUserWithEmailAndPassword(email.trim(), password).await().user!!
        runCatching { user.sendEmailVerification().await() }
        user.toAccount()
    }

    suspend fun sendPasswordReset(email: String) = firebase {
        requireAuth().sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    suspend fun signInWithGoogle(activity: Activity): Account {
        val auth = requireAuth()
        // Generated from google-services.json by the Google Services Gradle plugin.
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) throw AuthException("Google sign-in isn't set up: enable it in Firebase and download a fresh google-services.json.")
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(context.getString(resId)).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw AuthException("Sign-in cancelled.")
        } catch (e: GetCredentialException) {
            throw AuthException("Google sign-in failed: ${e.message ?: e.type}")
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException("Unexpected sign-in response.")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return firebase { auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user!!.toAccount() }
    }

    suspend fun signOut() {
        auth?.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun requireAuth() = auth ?: throw AuthException("Login isn't configured in this build.")

    private inline fun <T> firebase(block: () -> T): T = try {
        block()
    } catch (e: FirebaseNetworkException) {
        throw AuthException("No internet connection.")
    } catch (e: FirebaseAuthException) {
        throw AuthException(friendly(e.errorCode, e.message))
    }

    private fun friendly(code: String, fallback: String?) = when (code) {
        "ERROR_INVALID_EMAIL" -> "That email address doesn't look right."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Wrong email or password."
        "ERROR_USER_NOT_FOUND" -> "No account with that email. Create one instead."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Sign in instead."
        "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again in a few minutes."
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "This email is linked to Google sign-in. Use Continue with Google."
        else -> fallback ?: "Sign-in failed ($code)."
    }

    private fun FirebaseUser.toAccount() = Account(uid, email ?: displayName ?: "Signed in")
}
