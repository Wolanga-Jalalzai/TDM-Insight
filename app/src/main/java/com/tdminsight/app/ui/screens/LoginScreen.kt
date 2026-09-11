package com.tdminsight.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tdminsight.app.data.AuthStore
import com.tdminsight.app.ui.theme.Emerald600
import com.tdminsight.app.ui.theme.Red600
import com.tdminsight.app.ui.theme.Slate500
import com.tdminsight.app.ui.theme.Slate900

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
) {
    val context = LocalContext.current

    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun submit() {
        errorText = null
        if (username.isBlank() || password.isBlank()) {
            errorText = "Please fill in all fields."
            return
        }
        if (isRegisterMode) {
            if (password != confirmPassword) {
                errorText = "Passwords do not match."
                return
            }
            val err = AuthStore.register(context, username, password)
            if (err != null) errorText = err else onAuthenticated()
        } else {
            val err = AuthStore.login(context, username, password)
            if (err != null) errorText = err else onAuthenticated()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Slate900),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("TDM Insight", style = MaterialTheme.typography.headlineMedium, color = Slate900)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isRegisterMode) "Create an account to get started" else "Sign in to continue",
                color = Slate500,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorText = null },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorText = null },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isRegisterMode) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorText = null },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            errorText?.let { msg ->
                Spacer(Modifier.height(10.dp))
                Text(msg, color = Red600, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900)
            ) {
                Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isRegisterMode) "Create account" else "Sign in")
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = {
                isRegisterMode = !isRegisterMode
                errorText = null
                confirmPassword = ""
            }) {
                Text(
                    if (isRegisterMode) "Already have an account? Sign in"
                    else "New here? Create an account",
                    color = Emerald600
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Account data is stored locally on this device only — no data is sent to a server.",
                color = Slate500,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
