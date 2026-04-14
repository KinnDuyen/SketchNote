package com.example.sketchnote

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sketchnote.ui.editor.EditorScreen
import com.example.sketchnote.ui.editor.SketchScreen
import com.example.sketchnote.ui.home.HomeScreen
import com.example.sketchnote.ui.splash.SplashScreen
import com.example.sketchnote.ui.theme.SketchNoteTheme
import com.example.sketchnote.ui.trash.TrashScreen
import com.example.sketchnote.ui.backup.BackupScreen
import com.example.sketchnote.ui.biometric.BiometricScreen
import com.example.sketchnote.util.SketchResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SketchNoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "splash") {

                        composable("splash") {
                            SplashScreen(
                                onFinished = {
                                    navController.navigate("biometric") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("biometric") {
                            BiometricScreen(
                                onUnlocked = {
                                    navController.navigate("home") {
                                        popUpTo("biometric") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                onNoteClick = { noteId ->
                                    navController.navigate("editor/$noteId")
                                },
                                onCreateNote = { navController.navigate("editor/-1") },
                                onTrashClick = { navController.navigate("trash") },
                                onBackupClick = { navController.navigate("backup") }
                            )
                        }

                        composable(
                            route = "editor/{noteId}",
                            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                            EditorScreen(
                                noteId = noteId,
                                onBack = { navController.popBackStack() }
                                // ĐÃ XÓA onOpenSketch VÌ KHÔNG CÒN TRONG EditorScreen MỚI
                            )
                        }

                        composable("sketch") {
                            SketchScreen(
                                onBack = { navController.popBackStack() },
                                onSave = { savedPath: String ->
                                    SketchResult.pendingPath = savedPath
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("trash") {
                            TrashScreen(onBack = { navController.popBackStack() })
                        }

                        composable("backup") {
                            BackupScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}