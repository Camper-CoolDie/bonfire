package sh.sit.bonfire.auth.flows

import android.content.Context
import com.posthog.PostHog
import kotlinx.coroutines.MainScope
import sh.sit.bonfire.auth.AuthController
import sh.sit.bonfire.auth.LoginEmailMutation
import sh.sit.bonfire.auth.apollo
import sh.sit.bonfire.auth.integrity.IntegrityController
import sh.sit.schema.type.IntentionType
import sh.sit.schema.type.LoginEmailInput

class EmailAuthFlow(context: Context, val email: String, val password: String) : AuthFlow(context) {
    override suspend fun start() {
        PostHog.capture("auth_email")

        val response = try {
            apollo.mutation(LoginEmailMutation(LoginEmailInput(email = email, password = password)))
                .execute()
        } catch (e: Exception) {
            e.printStackTrace()
            throw AuthException(FailureReason.NetworkError)
        }

        if (response.hasErrors()) {
            throw AuthException.fromError(response.errors!!.first())
        }

        val data = response.dataAssertNoErrors.loginEmail

        when {
            data.onLoginResultSuccess != null -> {
                AuthController.saveAuthState(AuthController.AuthenticatedAuthState(
                    accessToken = data.onLoginResultSuccess.accessToken,
                    refreshToken = data.onLoginResultSuccess.refreshToken,
                    email = email,
                ))

                try {
                    IntegrityController.sendIntegrityToken(MainScope(), IntentionType.GENERIC)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            data.onLoginResultTfaRequired != null -> {
                AuthController.saveAuthState(AuthController.TfaAuthState(
                    tfaWaitToken = data.onLoginResultTfaRequired.tfaWaitToken,
                    tfaType = data.onLoginResultTfaRequired.tfaType,
                ))
            }
            else -> throw AuthException(FailureReason.Unknown, "Extra login result")
        }
    }
}
