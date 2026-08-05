package me.plexs.music.ui.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleAuth(context: Context, googleClientId: String) {

    val client: GoogleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(googleClientId)
            .requestEmail()
            .build(),
    )

    fun signInIntent(): Intent = client.signInIntent

    fun idToken(result: ActivityResult): String? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        return try {
            task.getResult(ApiException::class.java).idToken
        } catch (_: Exception) {
            null
        }
    }
}
