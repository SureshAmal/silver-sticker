package com.example.silversticker.ui.screens

import androidx.activity.compose.BackHandler

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.silversticker.models.StickerPack
import com.example.silversticker.ui.components.StaticStickerImage
import com.example.silversticker.ui.theme.SilverMotion
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPackListScreen(
    stickerPacks: List<StickerPack>,
    targetPackage: String,
    onTargetPackageChange: (String) -> Unit,
    onCreatePackClick: () -> Unit,
    onPackClick: (StickerPack) -> Unit,
    onRefresh: () -> Unit = {},
    onImportPackSelected: (Uri) -> Unit,
    onDeletePacks: (List<String>) -> Unit,
    onSharePacks: (List<String>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedPackIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var deletingPackIds by remember { mutableStateOf(setOf<String>()) }

    BackHandler(enabled = isSelectionMode) {
        selectedPackIds = emptySet()
        isSelectionMode = false
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImportPackSelected(it) }
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("StickerPackListScreen", "Screen shown, refreshing. Current count: ${stickerPacks.size}")
        onRefresh()
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedPackIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectedPackIds = emptySet()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedPackIds.isNotEmpty()) {
                                onSharePacks(selectedPackIds.toList())
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share selected packs")
                        }
                        IconButton(onClick = {
                            if (selectedPackIds.isNotEmpty()) {
                                coroutineScope.launch {
                                    deletingPackIds = deletingPackIds + selectedPackIds
                                    delay(380) // Smooth finish for collapse animation
                                    onDeletePacks(selectedPackIds.toList())
                                    selectedPackIds = emptySet()
                                    isSelectionMode = false
                                    deletingPackIds = emptySet()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected packs")
                        }
                    }
                )
            } else {
                LargeTopAppBar(
                    title = { Text("Silver Sticker") },
                    actions = {
                        IconButton(onClick = { importLauncher.launch("application/zip") }) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Import ZIP Pack"
                            )
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                if (targetPackage == "com.whatsapp") Icons.AutoMirrored.Filled.Chat else Icons.Default.Business,
                                contentDescription = "Target App"
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("WhatsApp") },
                                onClick = {
                                    onTargetPackageChange("com.whatsapp")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("WhatsApp Business") },
                                onClick = {
                                    onTargetPackageChange("com.whatsapp.w4b")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = onCreatePackClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create Pack") }
                )
            }
        }
    ) { padding ->
        if (stickerPacks.isEmpty()) {
            // Animated empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(SilverMotion.standard(320)) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = SilverMotion.expressiveSpring()
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val infiniteTransition = rememberInfiniteTransition(label = "empty")
                        val floatOffset by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 12f,
                            animationSpec = infiniteRepeatable(
                                animation = SilverMotion.standard(2200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "float"
                        )
                        Text(
                            text = "🎨",
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.graphicsLayer { translationY = -floatOffset }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No sticker packs yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap the button below to create one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(stickerPacks, key = { _, pack -> pack.identifier }) { index, pack ->
                    val isSelected = selectedPackIds.contains(pack.identifier)
                    val isDeleting = deletingPackIds.contains(pack.identifier)

                    StickerPackItem(
                        pack = pack,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        isDeleting = isDeleting,
                        onClick = {
                            if (isSelectionMode) {
                                selectedPackIds = if (isSelected) {
                                    selectedPackIds - pack.identifier
                                } else {
                                    selectedPackIds + pack.identifier
                                }
                                if (selectedPackIds.isEmpty()) {
                                    isSelectionMode = false
                                }
                            } else {
                                onPackClick(pack)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedPackIds = setOf(pack.identifier)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StickerPackItem(
    pack: StickerPack,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    // Press animation state
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.97f
            isSelected -> 0.99f
            else -> 1.0f
        },
        animationSpec = SilverMotion.quickSpring(),
        label = "pack_scale"
    )
    val elevation by animateFloatAsState(
        targetValue = when {
            isSelected -> 6f
            isPressed -> 4f
            else -> 1f
        },
        animationSpec = SilverMotion.standard(160),
        label = "pack_elevation"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = SilverMotion.standard(220),
        label = "pack_container"
    )

    AnimatedVisibility(
        visible = !isDeleting,
        exit = fadeOut(SilverMotion.emphasizedExit()) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = SilverMotion.emphasizedExit()
                ) +
                shrinkVertically(animationSpec = SilverMotion.emphasizedExit(260))
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { currentOnClick() },
                        onLongPress = { currentOnLongClick() }
                    )
                },
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val trayFile = pack.getTrayInternalFile(context)
                StaticStickerImage(
                    file = trayFile,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Fit,
                    targetSizePx = 96
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = pack.name, style = MaterialTheme.typography.titleLarge)
                    Text(text = pack.publisher, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${pack.stickers.size} stickers",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Animated Selection Checkbox
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 0.6f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
