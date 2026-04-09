package com.kazukittin.vault.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    loginState: LoginState,
    onLoginClick: (ip: String, account: String, pass: String) -> Unit,
    onSuccess: (sid: String) -> Unit
) {
    var nasIp by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val vaultSurface = Color(0xFF071327)
    val vaultPrimary = Color(0xFFA1CCED)

    // ログイン成功時にすぐに画面遷移（または成功アクション）を呼ぶ
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onSuccess(loginState.sid)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(vaultSurface),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF142034)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Vault NASへログイン",
                    style = MaterialTheme.typography.headlineMedium,
                    color = vaultPrimary
                )

                if (loginState is LoginState.Error) {
                    Text(
                        text = loginState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedTextField(
                    value = nasIp,
                    onValueChange = { nasIp = it },
                    label = { Text("NASのIPアドレス (例: 192.168.1.10)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = vaultPrimary,
                        unfocusedTextColor = Color.White
                    ),
                    enabled = loginState !is LoginState.Loading
                )

                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("ユーザー名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = vaultPrimary,
                        unfocusedTextColor = Color.White
                    ),
                    enabled = loginState !is LoginState.Loading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("パスワード") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = vaultPrimary,
                        unfocusedTextColor = Color.White
                    ),
                    enabled = loginState !is LoginState.Loading
                )

                Button(
                    onClick = { onLoginClick(nasIp, account, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = vaultPrimary),
                    enabled = loginState !is LoginState.Loading
                ) {
                    if (loginState is LoginState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = vaultSurface,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("ログイン", color = vaultSurface, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
