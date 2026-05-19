package com.example.silversticker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.silversticker.ui.StickerViewModel
import com.example.silversticker.ui.screens.CreateStickerPackScreen
import com.example.silversticker.ui.screens.StickerDetailsScreen
import com.example.silversticker.ui.screens.StickerPackListScreen
import com.example.silversticker.ui.theme.SilverStickerTheme
import com.example.silversticker.utils.StickerStorage
import com.example.silversticker.utils.WhatsAppUtil

import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure Coil globally to support decoding animated GIFs and WebPs
        val imageLoader = ImageLoader.Builder(applicationContext)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        StickerStorage.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            SilverStickerTheme {
                val navController = rememberNavController()
                val viewModel: StickerViewModel = viewModel()
                val stickerPacks by viewModel.stickerPacks.collectAsState()
                val targetPackage by viewModel.targetPackage.collectAsState()
                val context = LocalContext.current

                val addToWhatsAppLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    android.util.Log.d("MainActivity", "WhatsApp result: ${result.resultCode}")
                }

                NavHost(
                    navController = navController,
                    startDestination = "list",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    composable(
                        "list",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    ) {
                        StickerPackListScreen(
                            stickerPacks = stickerPacks,
                            targetPackage = targetPackage,
                            onTargetPackageChange = { viewModel.setTargetPackage(it) },
                            onCreatePackClick = { navController.navigate("create") },
                            onPackClick = { pack -> 
                                navController.navigate("details/${pack.identifier}") 
                            },
                            onRefresh = { viewModel.refreshPacks() },
                            onImportPackSelected = { uri ->
                                val imported = viewModel.importStickerPack(context, uri)
                                if (imported != null) {
                                    android.widget.Toast.makeText(context, "Imported '${imported.name}' successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to import pack. Ensure it's a valid pack ZIP.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onDeletePacks = { ids ->
                                viewModel.removeStickerPacks(ids)
                            },
                            onSharePacks = { ids ->
                                val uris = ArrayList<android.net.Uri>()
                                ids.forEach { id ->
                                    val pack = com.example.silversticker.utils.StickerStorage.getPack(id)
                                    if (pack != null) {
                                        val uri = viewModel.exportStickerPack(context, pack)
                                        if (uri != null) {
                                            uris.add(uri)
                                        }
                                    }
                                }
                                if (uris.isNotEmpty()) {
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "application/zip"
                                        putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Sticker Packs"))
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to export packs for sharing", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    composable(
                        "create",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up,
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Down,
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    ) {
                        CreateStickerPackScreen(
                            onBackClick = { navController.popBackStack() },
                            onSaveClick = { pack ->
                                viewModel.addStickerPack(pack)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        "details/{packId}",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    ) { backStackEntry ->
                        val packId = backStackEntry.arguments?.getString("packId")
                        val pack = stickerPacks.find { it.identifier == packId }
                        if (pack != null) {
                            StickerDetailsScreen(
                                pack = pack,
                                onBackClick = { navController.popBackStack() },
                                onAddToWhatsAppClick = {
                                    val intent = WhatsAppUtil.createAddStickerPackIntent(context, pack, targetPackage)
                                    if (intent != null) {
                                        addToWhatsAppLauncher.launch(intent)
                                    }
                                },
                                onDeletePackClick = {
                                    viewModel.removeStickerPack(pack.identifier)
                                    navController.popBackStack()
                                },
                                onAddStickers = { newStickers ->
                                    viewModel.addStickersToPack(pack.identifier, newStickers)
                                },
                                onRemoveStickers = { stickers ->
                                    viewModel.removeStickersFromPack(pack.identifier, stickers.map { it.imageFileName })
                                },
                                onUpdatePackDetails = { name, publisher, newTrayUri ->
                                    viewModel.updatePackDetails(pack.identifier, name, publisher, newTrayUri)
                                },
                                onUpdateStickerEmojis = { sticker, emojis ->
                                    viewModel.updateStickerEmojis(pack.identifier, sticker.imageFileName, emojis)
                                },
                                onSharePackClick = {
                                    val zipUri = viewModel.exportStickerPack(context, pack)
                                    if (zipUri != null) {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(android.content.Intent.EXTRA_STREAM, zipUri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Sticker Pack"))
                                    } else {
                                        android.widget.Toast.makeText(context, "Failed to export sticker pack", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
