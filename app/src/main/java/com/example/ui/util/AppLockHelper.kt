package com.example.ui.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/**
 * Zero-dependency native Android device lock authenticator supporting
 * Fingerprint / Biometrics and Device Screen PIN/Pattern/Password.
 */
object AppLockHelper {

    fun isDeviceSecure(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return false
        return keyguard.isDeviceSecure
    }

    @Suppress("DEPRECATION")
    fun authenticate(
        activity: Activity,
        title: String = "Unlock MonoDo",
        subtitle: String = "Use your fingerprint or device PIN to access your workspace",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFallbackCredentialIntent: (Intent) -> Unit
    ) {
        if (!isDeviceSecure(activity)) {
            onError("Set up a screen lock PIN or fingerprint in Android Settings first.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val builder = BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    builder.setDeviceCredentialAllowed(true)
                }

                val prompt = builder.build()
                val cancellationSignal = CancellationSignal()
                prompt.authenticate(
                    cancellationSignal,
                    activity.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            onError(errString?.toString() ?: "Authentication canceled")
                        }

                        override fun onAuthenticationFailed() {
                            // User can retry within system prompt
                        }
                    }
                )
                return
            } catch (_: Exception) {
                // Fall back to Keyguard confirm device credential intent below
            }
        }

        // API 24-28 or fallback: System PIN / Pattern / Fingerprint confirm credential screen
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val intent = keyguard?.createConfirmDeviceCredentialIntent(title, subtitle)
        if (intent != null) {
            onFallbackCredentialIntent(intent)
        } else {
            onError("Unable to launch device lock verification.")
        }
    }
}
