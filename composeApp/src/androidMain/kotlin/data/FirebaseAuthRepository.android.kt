package data

import android.content.Context
import com.agremision.veco.AppContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Implementación 'actual' para Android
actual suspend fun signOutGooglePlatform(): Result<Unit> {
    val context = AppContext.get()
    // Usa el MISMO Web Client ID que usaste en GoogleSignInHandler
    val webClientId = "32827173587-36n2s1c2vjp85br43o6000eev00c0hpl.apps.googleusercontent.com"
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    return suspendCoroutine { continuation ->
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(task.exception ?: RuntimeException("Error al cerrar sesión de Google")))
            }
        }
    }
}
