package com.unh.parkit_android26_myles_syed.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unh.parkit_android26_myles_syed.data.AccountRepository

@Composable
fun LoginScreen(onAuthenticated: () -> Unit) {
    var isNewAccount by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun submit() {
        error = null
        if (isNewAccount && password != confirm) {
            error = "Passwords don't match."
            return
        }
        loading = true
        val finished: (String?) -> Unit = { result ->
            loading = false
            if (result == null) onAuthenticated() else error = result
        }
        if (isNewAccount) AccountRepository.signUp(email.trim(), password, finished)
        else AccountRepository.signIn(email.trim(), password, finished)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (isNewAccount) "Create account" else "Welcome back",
            fontSize = 28.sp, fontWeight = FontWeight.Bold
        )
        Text(
            if (isNewAccount) "Create an account to save your vehicle and manage your campus parking."
            else "Sign in with your University account to manage your campus parking."
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, enabled = !loading, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, enabled = !loading, modifier = Modifier.fillMaxWidth()
        )
        if (isNewAccount) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it },
                label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, enabled = !loading, modifier = Modifier.fillMaxWidth()
            )
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { submit() },
            enabled = !loading && email.isNotBlank() && password.isNotBlank() &&
                (!isNewAccount || confirm.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                when {
                    loading -> "Please wait…"
                    isNewAccount -> "Create account"
                    else -> "Log in"
                }
            )
        }
        TextButton(
            onClick = {
                isNewAccount = !isNewAccount
                error = null
                confirm = ""
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isNewAccount) "Already have an account? Log in" else "New account? Sign up")
        }
    }
}
