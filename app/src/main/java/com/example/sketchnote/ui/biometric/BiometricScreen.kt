package com.example.sketchnote.ui.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var errorMsg by remember { mutableStateOf("") }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var isPromptShowing by remember { mutableStateOf(false) }

    // YC1 FIX: dùng callback state để tránh gọi navigation từ sai thread
    var shouldNavigate by remember { mutableStateOf(false) }

    // Khi shouldNavigate = true → gọi onUnlocked() trong Compose context (main thread an toàn)
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            shouldNavigate = false
            try {
                onUnlocked()
            } catch (e: Exception) {
                // ignore navigation errors
            }
        }
    }

    val authenticate = {
        if (!isPromptShowing) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                isPromptShowing = true
                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            isPromptShowing = false
                            // YC1 FIX: không gọi onUnlocked() trực tiếp, set flag để Compose xử lý
                            shouldNavigate = true
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            isPromptShowing = false
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                errorMsg = errString.toString()
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            isPromptShowing = false
                            errorMsg = "Máy không nhận ra vân tay này, thử lại nha cưng!"
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Xác thực chính chủ")
                    .setSubtitle("Vui lòng chạm đúng vân tay của cưng để vào SketchNote")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("Đóng App")
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        authenticate()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (errorMsg.isEmpty()) Icons.Default.Fingerprint else Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color(0xFFF9DC3B)
        )

        Spacer(Modifier.height(30.dp))

        Text(
            text = "SketchNote đang khóa",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = {
                errorMsg = ""
                authenticate()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Chạm để quét lại vân tay", color = Color.White)
        }
    }
}