package domain
import android.app.Activity.RESULT_OK
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope

private class GoogleSignInState(
    val googleSignInClient: GoogleSignInClient,
    val signInLauncher: ManagedActivityResultLauncher<android.content.Intent, ActivityResult>,
    val onResult: (GoogleSignInResult) -> Unit,
    val scope: CoroutineScope
) {
    fun launchSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    fun handleSignInResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                val accessToken = account.idToken
                if (idToken != null && accessToken != null) {
                    onResult(GoogleSignInResult(idToken = idToken, accessToken = accessToken, error = null))
                } else {
                    onResult(GoogleSignInResult(null, null, "No se obtuvieron los tokens de Google."))
                }
            } catch (e: ApiException) {
                val errorMessage = "Error de Google Sign-In: Código ${e.statusCode}"
                onResult(GoogleSignInResult(null, null, errorMessage))
            }
        } else {
            onResult(GoogleSignInResult(null, null, "Inicio de sesión cancelado o fallido (Código: ${result.resultCode})"))
        }
    }
}

@Composable
actual fun GoogleSignInHandler(
    triggerSignIn: Boolean,
    onResult: (GoogleSignInResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSignInClient = remember(context) {
        val webClientId = "32827173587-36n2s1c2vjp85br43o6000eev00c0hpl.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    var googleSignInState by remember { mutableStateOf<GoogleSignInState?>(null) }
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult() // Cambia el contrato
    ) { result ->
        googleSignInState?.handleSignInResult(result)
    }
    LaunchedEffect(googleSignInClient, signInLauncher, onResult, scope) {
        googleSignInState = GoogleSignInState(googleSignInClient, signInLauncher, onResult, scope)
    }
    LaunchedEffect(triggerSignIn) {
        if (triggerSignIn) {
            googleSignInState?.launchSignIn()
        }
    }
}
