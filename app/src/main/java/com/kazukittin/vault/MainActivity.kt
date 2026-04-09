package com.kazukittin.vault

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kazukittin.vault.data.local.VaultAuthManager
import com.kazukittin.vault.data.repository.AuthRepository
import com.kazukittin.vault.ui.home.HomeScreen
import com.kazukittin.vault.ui.login.LoginScreen
import com.kazukittin.vault.ui.login.LoginViewModel
import com.kazukittin.vault.ui.theme.VaultTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authManager = VaultAuthManager(applicationContext)
        val authRepository = AuthRepository(authManager)

        val savedIp = authManager.getNasIp() ?: "192.168.10.115"
        val photosApi = com.kazukittin.vault.data.remote.VaultNetworkClient.createPhotosApi(applicationContext, savedIp)
        val db = com.kazukittin.vault.data.local.db.VaultDatabase.getDatabase(applicationContext)
        val folderRepository = com.kazukittin.vault.data.repository.FolderRepository(authManager, photosApi, db.folderDao())

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return LoginViewModel(authRepository) as T
                }
                if (modelClass.isAssignableFrom(com.kazukittin.vault.ui.home.HomeViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return com.kazukittin.vault.ui.home.HomeViewModel(folderRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        enableEdgeToEdge()
        setContent {
            VaultTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDest = if (authManager.getSessionId() != null) "home" else "login"

                    NavHost(navController = navController, startDestination = startDest) {

                        composable("login") {
                            val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                            val loginState by loginViewModel.loginState.collectAsState()
                            LoginScreen(
                                loginState = loginState,
                                onLoginClick = { ip, account, pass ->
                                    loginViewModel.login(applicationContext, ip, account, pass)
                                },
                                onSuccess = { _ ->
                                    Toast.makeText(this@MainActivity, "ログイン成功!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            val homeViewModel: com.kazukittin.vault.ui.home.HomeViewModel =
                                viewModel(factory = viewModelFactory)
                            val allFolders by homeViewModel.allFolders.collectAsState()
                            val pinnedFolders by homeViewModel.pinnedFolders.collectAsState()
                            HomeScreen(
                                pinnedCollections = pinnedFolders,
                                allCollections = allFolders,
                                onFolderClick = { path, name ->
                                    val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                                    val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
                                    navController.navigate("folder/$encodedPath?name=$encodedName")
                                }
                            )
                        }

                        composable(
                            route = "folder/{folderPath}?name={folderName}",
                            arguments = listOf(
                                androidx.navigation.navArgument("folderPath") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("folderName") { type = androidx.navigation.NavType.StringType; defaultValue = "Folder" }
                            )
                        ) { backStackEntry ->
                            val folderPath = backStackEntry.arguments?.getString("folderPath") ?: ""
                            val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
                            val decodedPath = java.net.URLDecoder.decode(folderPath, "UTF-8")
                            val decodedName = java.net.URLDecoder.decode(folderName, "UTF-8")

                            val folderViewModel: com.kazukittin.vault.ui.folder.FolderContentViewModel = viewModel(
                                key = decodedPath,
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return com.kazukittin.vault.ui.folder.FolderContentViewModel(folderRepository, decodedPath) as T
                                    }
                                }
                            )

                            com.kazukittin.vault.ui.folder.FolderContentScreen(
                                folderName = decodedName,
                                viewModel = folderViewModel,
                                onBack = { navController.popBackStack() },
                                onFolderClick = { path, name ->
                                    val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                                    val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
                                    navController.navigate("folder/$encodedPath?name=$encodedName")
                                },
                                onPhotoClick = { index ->
                                    val encodedPath = java.net.URLEncoder.encode(decodedPath, "UTF-8")
                                    navController.navigate("photo?folderPath=$encodedPath&startIndex=$index")
                                }
                            )
                        }

                        composable(
                            route = "photo?folderPath={folderPath}&startIndex={startIndex}",
                            arguments = listOf(
                                androidx.navigation.navArgument("folderPath") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("startIndex") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
                            )
                        ) { backStackEntry ->
                            val folderPath = backStackEntry.arguments?.getString("folderPath") ?: ""
                            val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0
                            val decodedPath = java.net.URLDecoder.decode(folderPath, "UTF-8")

                            val folderViewModel: com.kazukittin.vault.ui.folder.FolderContentViewModel = viewModel(
                                key = decodedPath,
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return com.kazukittin.vault.ui.folder.FolderContentViewModel(folderRepository, decodedPath) as T
                                    }
                                }
                            )

                            com.kazukittin.vault.ui.viewer.PhotoViewerScreen(
                                viewModel = folderViewModel,
                                initialIndex = startIndex,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}