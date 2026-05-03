package com.kazukittin.vault.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    loginState: LoginState,
    isScanning: Boolean,
    discoveredNases: List<String>,
    onLoginClick: (ip: String, account: String, pass: String) -> Unit,
    onStartDiscovery: () -> Unit,
    onSuccess: (sid: String) -> Unit
) {
    var nasIp by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val vaultSurface = Color(0xFF071327)
    val vaultPrimary = Color(0xFFA1CCED)
    val vaultContainer = Color(0xFF142034)

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
            colors = CardDefaults.cardColors(containerColor = vaultContainer),
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

                // IPアドレス入力 + 自動検出ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nasIp,
                        onValueChange = { nasIp = it },
                        label = { Text("NASのIPアドレス") },
                        placeholder = { Text("例: 192.168.1.10", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = vaultPrimary,
                            unfocusedTextColor = Color.White
                        ),
                        enabled = loginState !is LoginState.Loading
                    )

                    OutlinedButton(
                        onClick = onStartDiscovery,
                        enabled = !isScanning && loginState !is LoginState.Loading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = vaultPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = vaultPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("自動検出", fontSize = 12.sp)
                        }
                    }
                }

                // 検出中メッセージ
                AnimatedVisibility(visible = isScanning) {
                    Text(
                        text = "ネットワークをスキャン中...",
                        color = vaultPrimary.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // 検出されたNASのリスト
                AnimatedVisibility(visible = discoveredNases.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "見つかったNAS",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        discoveredNases.forEach { ip ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { nasIp = ip },
                                color = if (nasIp == ip) vaultPrimary.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ip, color = Color.White, fontSize = 14.sp)
                                    Text(
                                        "選択",
                                        color = vaultPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

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
