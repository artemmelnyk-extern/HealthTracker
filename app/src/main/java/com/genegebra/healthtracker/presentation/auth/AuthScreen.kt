package com.genegebra.healthtracker.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.loggedInUser) {
        if (uiState.loggedInUser != null) onAuthenticated()
    }

    if (uiState.pendingEmailVerification) {
        EmailVerificationScreen(
            email = uiState.pendingEmail,
            onVerifiedClick = { viewModel.checkEmailVerified() },
            onResendClick = { viewModel.resendVerificationEmail() },
            isLoading = uiState.isLoading,
            error = uiState.error,
            onClearError = { viewModel.clearError() }
        )
        return
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (uiState.isRegisterMode) "Create Account" else "Sign In",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.isRegisterMode) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (!viewModel.isValidEmail(email)) {
                    viewModel.setError("Enter a valid email address.")
                } else if (uiState.isRegisterMode) {
                    if (password == confirmPassword) {
                        viewModel.register(email.trim(), password)
                    } else {
                        viewModel.setError("Passwords do not match.")
                    }
                } else {
                    viewModel.login(email.trim(), password)
                }
            },
            enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (uiState.isRegisterMode) "Register" else "Login")
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = { viewModel.toggleMode() }) {
            Text(if (uiState.isRegisterMode) "Already have an account? Sign in" else "No account? Create one")
        }
    }
}

@Composable
private fun EmailVerificationScreen(
    email: String,
    onVerifiedClick: () -> Unit,
    onResendClick: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Verify your email", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "A verification link has been sent to $email. Click the link then return here.",
            style = MaterialTheme.typography.bodyMedium
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onVerifiedClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("I've verified my email")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onResendClick) {
            Text("Resend verification email")
        }
    }
}
