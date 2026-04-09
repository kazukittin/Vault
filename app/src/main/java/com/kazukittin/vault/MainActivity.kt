package com.kazukittin.vault

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kazukittin.vault.data.local.VaultAuthManager
import com.kazukittin.vault.data.repository.AuthRepository
import com.kazukittin.vault.ui.theme.VaultTheme
import com.kazukittin.vault.ui.login.LoginScreen
import com.kazukittin.vault.ui.login.LoginViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 簡易DI: 手動で依存関係を解決（本来はHilt等を使います）
        val authManager = VaultAuthManager(applicationContext)
        val authRepository = AuthRepository(authManager)
        
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return LoginViewModel(authRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        enableEdgeToEdge()
        setContent {
            VaultTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        
                        // Compose内でViewModelを取得
                        val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                        val loginState by loginViewModel.loginState.collectAsState()

                        LoginScreen(
                            loginState = loginState,
                            onLoginClick = { ip, account, pass ->
                                // ViewModelにログイン処理を移譲
                                loginViewModel.login(applicationContext, ip, account, pass)
                            },
                            onSuccess = { sid ->
                                // ログイン成功時：本来はここでHomeScreenに遷移する
                                Toast.makeText(this@MainActivity, "ログイン成功! SID: $sid", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }
}