package com.kazukittin.vault.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(context: Context, ip: String, account: String, pass: String) {
        if (ip.isBlank() || account.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("すべての項目を入力してください")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val result = repository.loginFirstTime(ip, account, pass)
            
            result.onSuccess { sid ->
                _loginState.value = LoginState.Success(sid)
            }
            result.onFailure { error ->
                _loginState.value = LoginState.Error(error.message ?: "通信エラーが発生しました")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val sid: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
