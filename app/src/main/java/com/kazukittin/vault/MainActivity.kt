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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kazukittin.vault.ui.audio.MiniPlayer
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
        val authRepository = AuthRepository(authManager, applicationContext)

        val savedIp = authManager.getNasIp() ?: "192.168.10.115"
        val photosApi = com.kazukittin.vault.data.remote.VaultNetworkClient.createPhotosApi(applicationContext, savedIp)
        val dlSiteApi = com.kazukittin.vault.data.remote.VaultNetworkClient.createDlSiteApi()
        val db = com.kazukittin.vault.data.local.db.VaultDatabase.getDatabase(applicationContext)
        val folderRepository = com.kazukittin.vault.data.repository.FolderRepository(authManager, photosApi, dlSiteApi, db.folderDao(), db.dlSiteCacheDao(), authRepository)
        val audioRepository = com.kazukittin.vault.data.repository.AudioRepository(authManager, photosApi, dlSiteApi, db.audioDao(), authRepository)

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return when {
                    modelClass.isAssignableFrom(LoginViewModel::class.java) -> 
                        LoginViewModel(authRepository) as T
                    modelClass.isAssignableFrom(com.kazukittin.vault.ui.home.HomeViewModel::class.java) -> 
                        com.kazukittin.vault.ui.home.HomeViewModel(folderRepository, authRepository) as T
                    modelClass.isAssignableFrom(com.kazukittin.vault.ui.audio.AudioLibraryViewModel::class.java) -> 
                        com.kazukittin.vault.ui.audio.AudioLibraryViewModel(audioRepository) as T
                    modelClass.isAssignableFrom(com.kazukittin.vault.ui.audio.AudioPlayerViewModel::class.java) -> 
                        com.kazukittin.vault.ui.audio.AudioPlayerViewModel(audioRepository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            VaultTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDest = if (authManager.getSessionId() != null) "home" else "login"

                    // フォルダ/写真ビューアーで共有するViewModel（同じインスタンスを使うことでインデックスを正しく共有）
                    val folderViewModel: com.kazukittin.vault.ui.folder.FolderContentViewModel =
                        viewModel(factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return com.kazukittin.vault.ui.folder.FolderContentViewModel(
                                    folderRepository, db.mangaBookmarkDao()
                                ) as T
                            }
                        })

                    // AudioPlayerViewModel is shared
                    val audioPlayerViewModel: com.kazukittin.vault.ui.audio.AudioPlayerViewModel = viewModel(factory = viewModelFactory)
                    audioPlayerViewModel.initController(applicationContext)

                    val hasActiveSession by audioPlayerViewModel.hasActiveSession.collectAsState()
                    var isPlayerMinimized by remember { mutableStateOf(false) }

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
                                    val folder = (pinnedFolders + allFolders).find { it.id == path }
                                    val cat = folder?.category ?: ""
                                    val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                                    val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
                                    val encodedCat = java.net.URLEncoder.encode(cat, "UTF-8")
                                    navController.navigate("folder/$encodedPath?name=$encodedName&cat=$encodedCat")
                                },
                                onSetCategory = { id, cat -> homeViewModel.setFolderCategory(id, cat) }
                            )
                        }

                        composable(
                            route = "folder/{folderPath}?name={folderName}&cat={category}",
                            arguments = listOf(
                                androidx.navigation.navArgument("folderPath") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("folderName") { type = androidx.navigation.NavType.StringType; defaultValue = "Folder" },
                                androidx.navigation.navArgument("category") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            val folderPath = backStackEntry.arguments?.getString("folderPath") ?: ""
                            val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
                            val category = backStackEntry.arguments?.getString("category") ?: ""
                            val decodedPath = java.net.URLDecoder.decode(folderPath, "UTF-8")
                            val decodedName = java.net.URLDecoder.decode(folderName, "UTF-8")
                            val decodedCat = java.net.URLDecoder.decode(category, "UTF-8").ifEmpty { null }

                            // 共有VMに新しいフォルダパスを伝える
                            androidx.compose.runtime.LaunchedEffect(decodedPath) {
                                folderViewModel.navigateTo(decodedPath, decodedCat)
                            }

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
                                },
                                onZipClick = { path, name ->
                                    val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                                    val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
                                    navController.navigate("manga_reader?path=$encodedPath&name=$encodedName")
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
                            val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0

                            // フォルダ画面と同じVMを使うことで、同じitemsリストを参照しインデックスが正確に一致する
                            com.kazukittin.vault.ui.viewer.PhotoViewerScreen(
                                viewModel = folderViewModel,
                                audioPlayerViewModel = audioPlayerViewModel,
                                initialIndex = startIndex,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "manga_reader?path={path}&name={name}",
                            arguments = listOf(
                                androidx.navigation.navArgument("path") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("name") { type = androidx.navigation.NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                            val name = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", "UTF-8")
                            val decodedPath = java.net.URLDecoder.decode(encodedPath, "UTF-8")

                            // ZIPのダウンロードURLを構築
                            val ip = authManager.getNasIp() ?: "192.168.10.115"
                            val sid = authManager.getSessionId() ?: ""
                            val pathParam = decodedPath.split("/").joinToString("/") {
                                java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")
                            }
                            val downloadUrl = "http://$ip:5000/webapi/entry.cgi?api=SYNO.FileStation.Download&version=2&method=download&path=$pathParam&mode=download&_sid=$sid"

                            val mangaViewModel: com.kazukittin.vault.ui.manga.MangaReaderViewModel =
                                androidx.lifecycle.viewmodel.compose.viewModel(
                                    key = decodedPath,
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            @Suppress("UNCHECKED_CAST")
                                            return com.kazukittin.vault.ui.manga.MangaReaderViewModel(
                                                db.mangaBookmarkDao()
                                            ) as T
                                        }
                                    }
                                )

                            com.kazukittin.vault.ui.manga.MangaReaderScreen(
                                viewModel = mangaViewModel,
                                downloadUrl = downloadUrl,
                                zipName = name,
                                zipPath = decodedPath,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("audio_library") {
                            com.kazukittin.vault.ui.audio.AudioLibraryScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                onWorkClick = { work ->
                                    val encoded = java.net.URLEncoder.encode(work.folderPath, "UTF-8")
                                    navController.navigate("audio_detail/$encoded")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("audio_detail/{workPath}") { backStackEntry ->
                            val workPath = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("workPath") ?: "", "UTF-8")
                            
                            // Initialize with a factory to fetch the WorkEntity first or pass workPath to VM
                            val detailViewModel: com.kazukittin.vault.ui.audio.AudioDetailViewModel = viewModel(
                                key = workPath,
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return com.kazukittin.vault.ui.audio.AudioDetailViewModel(audioRepository, workPath) as T
                                    }
                                }
                            )

                            com.kazukittin.vault.ui.audio.AudioDetailScreen(
                                viewModel = detailViewModel,
                                onTrackClick = { tracks, index ->
                                    val work = detailViewModel.work.value ?: run {
                                        Toast.makeText(this@MainActivity, "ERROR: Work data null", Toast.LENGTH_SHORT).show()
                                        return@AudioDetailScreen
                                    }
                                    Toast.makeText(this@MainActivity, "曲をタップしました: ${tracks[index].title}", Toast.LENGTH_SHORT).show()
                                    android.util.Log.wtf("VaultDebug", ">>> TRACK CLICKED: ${tracks[index].title}")
                                    audioPlayerViewModel.playTracks(work, tracks, index)
                                    Toast.makeText(this@MainActivity, "再生画面へ遷移します...", Toast.LENGTH_SHORT).show()
                                    android.util.Log.wtf("VaultDebug", ">>> NAVIGATING TO PLAYER")
                                    isPlayerMinimized = false
                                    navController.navigate("audio_player")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("audio_player") {
                            com.kazukittin.vault.ui.audio.AudioPlayerScreen(
                                viewModel = audioPlayerViewModel,
                                onBack = {
                                    isPlayerMinimized = true
                                    navController.popBackStack()
                                },
                                onMinimize = {
                                    isPlayerMinimized = true
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    // ミニプレイヤー：音声セッション中かつ最小化状態のとき表示
                    if (hasActiveSession && isPlayerMinimized) {
                        MiniPlayer(
                            viewModel = audioPlayerViewModel,
                            onExpand = {
                                isPlayerMinimized = false
                                navController.navigate("audio_player")
                            },
                            onClose = {
                                audioPlayerViewModel.stop()
                                isPlayerMinimized = false
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}