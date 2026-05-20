package com.example.silversticker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import android.net.Uri
import android.content.ContentValues
import android.provider.MediaStore
import android.content.Intent
import androidx.core.content.FileProvider
import android.widget.Toast
import android.os.Build
import android.util.Log
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.silversticker.models.Sticker
import com.example.silversticker.models.StickerPack
import com.example.silversticker.ui.components.StaticStickerImage
import com.example.silversticker.ui.theme.SilverMotion
import com.example.silversticker.utils.ImageUtils
import com.example.silversticker.utils.StickerStorage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerDetailsScreen(
    pack: StickerPack,
    onBackClick: () -> Unit,
    onAddToWhatsAppClick: () -> Unit,
    onDeletePackClick: () -> Unit,
    onAddStickers: (List<Sticker>) -> Unit,
    onRemoveStickers: (List<Sticker>) -> Unit,
    onSharePackClick: () -> Unit,
    onUpdatePackDetails: (String, String, Uri?) -> Unit,
    onUpdateStickerEmojis: (Sticker, List<String>) -> Unit
) {
    val context = LocalContext.current
    var selectedSticker by remember { mutableStateOf<Sticker?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStickerDeleteDialog by remember { mutableStateOf<List<Sticker>?>(null) }

    // Multi-Selection State for stickers
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedStickers by remember { mutableStateOf(setOf<Sticker>()) }

    // Edit Pack Details State
    var showEditPackDialog by remember { mutableStateOf(false) }
    var editName by remember(pack.name) { mutableStateOf(pack.name) }
    var editPublisher by remember(pack.publisher) { mutableStateOf(pack.publisher) }
    var selectedNewTrayUri by remember { mutableStateOf<Uri?>(null) }
    var isDestroyingPack by remember { mutableStateOf(false) }

    val trayPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedNewTrayUri = uri
        }
    }

    // Sticker Processing loading state
    var isProcessingStickers by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = isSelectionMode) {
        selectedStickers = emptySet()
        isSelectionMode = false
    }

    val gridState = rememberLazyGridState()
    val collapseFraction by remember {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val offset = gridState.firstVisibleItemScrollOffset.toFloat()
                (offset / 240f).coerceIn(0f, 1f)
            }
        }
    }

    val stickerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isProcessingStickers = true
            processingMessage = "Preparing stickers..."
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val total = uris.size
                    val webpStickers = uris.mapIndexedNotNull { index, uri ->
                        withContext(Dispatchers.Main) {
                            processingMessage = "Converting sticker ${index + 1} of $total..."
                        }
                        val baseName = "sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
                        ImageUtils.convertToWebP(context, uri, 512, baseName)?.let { result ->
                            Sticker(result.file.name, isAnimated = result.isAnimated)
                        }
                    }
                    if (webpStickers.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onAddStickers(webpStickers)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StickerDetailsScreen", "Error processing stickers: ${e.message}", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        isProcessingStickers = false
                        processingMessage = ""
                    }
                }
            }
        }
    }

    // Material 3 Loader overlay for sticker generation/conversion
    if (isProcessingStickers) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {}
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Generating Stickers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = processingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // Edit Pack Details Dialog (Renames name, publisher, and replaces tray icon WebP)
    if (showEditPackDialog) {
        AlertDialog(
            modifier = Modifier.padding(16.dp),
            onDismissRequest = { 
                showEditPackDialog = false 
                selectedNewTrayUri = null
            },
            title = { Text("Edit Pack Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Visual Tray Icon selection box with clean, bright preview and floating edit badge
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clickable { trayPicker.launch("image/*") }
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (selectedNewTrayUri != null) {
                                    AsyncImage(
                                        model = selectedNewTrayUri,
                                        contentDescription = "New Tray Icon",
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    val trayFile = pack.getTrayInternalFile(context)
                                    StaticStickerImage(
                                        file = trayFile,
                                        contentDescription = "Current Tray Icon",
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        contentScale = ContentScale.Fit,
                                        targetSizePx = 96
                                    )
                                }
                            }
                        }

                        // Floating Pencil Edit Badge at corner
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change Tray Icon",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        "Tap to change tray icon (96x96 WebP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Pack Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPublisher,
                        onValueChange = { editPublisher = it },
                        label = { Text("Publisher") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank() && editPublisher.isNotBlank()) {
                            onUpdatePackDetails(editName, editPublisher, selectedNewTrayUri)
                            showEditPackDialog = false
                            selectedNewTrayUri = null
                        } else {
                            Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showEditPackDialog = false 
                        selectedNewTrayUri = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    // Delete Pack Dialog (Styled with padding & bodyMedium text to prevent width overflow on compact screens)
    if (showDeleteDialog) {
        AlertDialog(
            modifier = Modifier.padding(16.dp),
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Pack") },
            text = { Text("Are you sure you want to delete '${pack.name}'? This action cannot be undone.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        isDestroyingPack = true
                        coroutineScope.launch {
                            delay(SilverMotion.Medium1.toLong())
                            onDeletePackClick()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Sticker(s) Dialog (Supports single & multiple sticker deletion)
    showStickerDeleteDialog?.let { stickersToDelete ->
        AlertDialog(
            modifier = Modifier.padding(16.dp),
            onDismissRequest = { showStickerDeleteDialog = null },
            title = { Text(if (stickersToDelete.size > 1) "Remove Stickers" else "Remove Sticker") },
            text = {
                Text(
                    if (stickersToDelete.size > 1) "Remove ${stickersToDelete.size} selected stickers from the pack?"
                    else "Remove this sticker from the pack?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveStickers(stickersToDelete)
                        showStickerDeleteDialog = null
                        selectedSticker = null
                        selectedStickers = emptySet()
                        isSelectionMode = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showStickerDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    AnimatedVisibility(
        visible = !isDestroyingPack,
        exit = fadeOut(SilverMotion.emphasizedExit(SilverMotion.Short4)) +
                slideOutVertically(
                    targetOffsetY = { it / 5 },
                    animationSpec = SilverMotion.emphasizedExit(SilverMotion.Short4)
                ) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = SilverMotion.emphasizedExit(SilverMotion.Short4)
                )
    ) {
    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedStickers.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectedStickers = emptySet()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedStickers.isNotEmpty()) {
                                shareMultipleStickers(context, selectedStickers.toList())
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share selected stickers")
                        }
                        IconButton(onClick = {
                            if (selectedStickers.isNotEmpty()) {
                                showStickerDeleteDialog = selectedStickers.toList()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected stickers")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .graphicsLayer {
                                    // Slide up and fade in collapsed TopAppBar elements based on scroll fraction
                                    alpha = collapseFraction
                                    translationY = (1f - collapseFraction) * 35f
                                }
                        ) {
                            val trayFile = pack.getTrayInternalFile(context)
                            StaticStickerImage(
                                file = trayFile,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Fit,
                                targetSizePx = 72
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pack.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    "By ${pack.publisher}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showEditPackDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Pack Details")
                        }
                        IconButton(onClick = onSharePackClick) {
                            Icon(Icons.Default.Share, contentDescription = "Share Sticker Pack")
                        }
                        IconButton(onClick = { stickerPicker.launch("image/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Sticker")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Pack")
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Button(
                    onClick = onAddToWhatsAppClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ADD TO WHATSAPP", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Animated header
                item(span = { GridItemSpan(4) }) {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(SilverMotion.standard(260)) + slideInVertically(
                            initialOffsetY = { -it / 5 },
                            animationSpec = SilverMotion.emphasizedEnter()
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 16.dp, top = 8.dp)
                                .graphicsLayer {
                                    // Slide up and fade out original header based on scroll collapse fraction
                                    alpha = 1f - collapseFraction
                                    translationY = -collapseFraction * 40f
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val trayFile = pack.getTrayInternalFile(context)
                            StaticStickerImage(
                                file = trayFile,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Fit,
                                targetSizePx = 120
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = pack.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${pack.stickers.size} Stickers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Sticker grid stays virtualized; motion is limited to targeted micro-interactions.
                itemsIndexed(
                    pack.stickers,
                    key = { _, sticker -> sticker.imageFileName }
                ) { index, sticker ->
                    val isSelected = selectedStickers.contains(sticker)
                    StickerGridItem(
                        sticker = sticker,
                        modifier = Modifier.animateItem(),
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                selectedStickers = if (isSelected) {
                                    selectedStickers - sticker
                                } else {
                                    selectedStickers + sticker
                                }
                                if (selectedStickers.isEmpty()) {
                                    isSelectionMode = false
                                }
                            } else {
                                selectedSticker = sticker
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedStickers = setOf(sticker)
                            }
                        }
                    )
                }
            }

            StickerPreviewOverlay(
                sticker = selectedSticker,
                onDismiss = { selectedSticker = null },
                onDelete = { showStickerDeleteDialog = listOf(it) },
                onUpdateEmojis = { updatedSticker, newEmojis ->
                    onUpdateStickerEmojis(updatedSticker, newEmojis)
                    selectedSticker = updatedSticker.copy(emojis = newEmojis)
                }
            )
        }
    }
    }
}

/**
 * Individual sticker grid item optimized for smooth scrolling.
 */
@Composable
private fun StickerGridItem(
    sticker: Sticker,
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    // Press state
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.93f
            isSelected -> 0.97f
            else -> 1f
        },
        animationSpec = SilverMotion.pressSpring(),
        label = "press_scale"
    )
    val stickerScale by animateFloatAsState(
        targetValue = if (isPressed) 1.05f else 1f,
        animationSpec = SilverMotion.pressSpring(),
        label = "sticker_inner_scale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = SilverMotion.standard(180),
        label = "sticker_container"
    )
    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 18.dp
            isSelected -> 20.dp
            else -> 12.dp
        },
        animationSpec = SilverMotion.pressSpring(),
        label = "sticker_shape"
    )

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        tonalElevation = if (isPressed) 3.dp else 0.dp,
        shadowElevation = if (isPressed) 3.dp else 0.dp,
        color = containerColor,
        border = if (isSelectionMode && isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .aspectRatio(1f)
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
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StaticStickerImage(
                file = sticker.getInternalFile(context),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = stickerScale
                        scaleY = stickerScale
                    }
                    .padding(6.dp),
                targetSizePx = 160
            )

            if (sticker.emojis.isNotEmpty()) {
                val emojiText = sticker.emojis.joinToString("")
                if (emojiText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = emojiText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(SilverMotion.standard(120)) + scaleIn(
                    initialScale = 0.65f,
                    animationSpec = SilverMotion.pressSpring()
                ),
                exit = fadeOut(SilverMotion.standard(90)) + scaleOut(
                    targetScale = 0.65f,
                    animationSpec = SilverMotion.quickSpring()
                ),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Telegram-style sticker preview overlay with morph scale, dim background,
 * and one-shot physics settling on the sticker.
 */
@Composable
private fun StickerPreviewOverlay(
    sticker: Sticker?,
    onDismiss: () -> Unit,
    onDelete: (Sticker) -> Unit,
    onUpdateEmojis: (Sticker, List<String>) -> Unit
) {
    val context = LocalContext.current
    val isVisible = sticker != null

    val previewTransition = updateTransition(targetState = isVisible, label = "preview")
    val dimAlpha by previewTransition.animateFloat(
        transitionSpec = { if (targetState) SilverMotion.standard(220) else SilverMotion.emphasizedExit(160) },
        label = "dim"
    ) { visible -> if (visible) 1f else 0f }
    val contentScale by previewTransition.animateFloat(
        transitionSpec = { if (targetState) SilverMotion.spatialSpring() else SilverMotion.emphasizedExit(160) },
        label = "morph_scale"
    ) { visible -> if (visible) 1f else 0.88f }
    val contentAlpha by previewTransition.animateFloat(
        transitionSpec = { if (targetState) SilverMotion.standard(160) else SilverMotion.emphasizedExit(120) },
        label = "morph_alpha"
    ) { visible -> if (visible) 1f else 0f }
    val offsetY by previewTransition.animateFloat(
        transitionSpec = { if (targetState) SilverMotion.spatialSpring() else SilverMotion.emphasizedExit(160) },
        label = "slide_y"
    ) { visible -> if (visible) 0f else 72f }

    var lastActiveSticker by remember { mutableStateOf<Sticker?>(null) }
    LaunchedEffect(sticker) {
        if (sticker != null) {
            lastActiveSticker = sticker
        }
    }

    if (isVisible || dimAlpha > 0.01f) {
        if (isVisible) {
            BackHandler { onDismiss() }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * dimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            lastActiveSticker?.let { currentSticker ->
                val file = currentSticker.getInternalFile(context)
                val fileSizeKb = if (file.exists()) file.length() / 1024 else 0
                val stickerPop = remember { Animatable(0.94f) }

                LaunchedEffect(currentSticker.imageFileName, isVisible) {
                    if (isVisible) {
                        stickerPop.snapTo(0.94f)
                        stickerPop.animateTo(1f, animationSpec = SilverMotion.spatialSpring())
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .graphicsLayer {
                            scaleX = contentScale
                            scaleY = contentScale
                            alpha = contentAlpha
                            translationY = offsetY
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 16.dp,
                    shadowElevation = 24.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AsyncImage(
                            model = file,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(240.dp)
                                .graphicsLayer {
                                    scaleX = stickerPop.value
                                    scaleY = stickerPop.value
                                }
                                .padding(16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentSticker.imageFileName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentSticker.emojis.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentSticker.emojis.joinToString(""),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Text(
                            text = "${fileSizeKb}KB • WebP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Download button
                            Button(
                                onClick = { downloadSticker(context, currentSticker) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save")
                            }

                            // Share button
                            Button(
                                onClick = { shareSticker(context, currentSticker) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        var showEmojiDialog by remember { mutableStateOf(false) }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showEmojiDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Emojis")
                            }
                            
                            OutlinedButton(
                                onClick = { onDelete(currentSticker) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Remove")
                            }
                        }
                        
                        if (showEmojiDialog) {
                            var emojiInput by remember { mutableStateOf(currentSticker.emojis.joinToString("")) }
                            Dialog(
                                onDismissRequest = { showEmojiDialog = false },
                                properties = DialogProperties(decorFitsSystemWindows = false)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .imePadding(),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Text("Search Emojis", style = MaterialTheme.typography.titleLarge)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedTextField(
                                            value = emojiInput,
                                            onValueChange = { emojiInput = ImageUtils.filterEmojiInput(it) },
                                            label = { Text("Up to 3 Emojis") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { showEmojiDialog = false }) { Text("Cancel") }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            TextButton(onClick = {
                                                val newEmojis = ImageUtils.splitIntoEmojis(emojiInput)
                                                onUpdateEmojis(currentSticker, newEmojis)
                                                showEmojiDialog = false
                                            }) { Text("Save") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun downloadSticker(context: Context, sticker: Sticker) {
    try {
        val webpFile = sticker.getInternalFile(context)
        val gifFile = java.io.File(webpFile.parentFile, sticker.imageFileName.replace(".webp", ".gif"))
        val isAnimated = sticker.isAnimated && gifFile.exists()
        val srcFile = if (isAnimated) gifFile else webpFile
        
        if (!srcFile.exists()) {
            Toast.makeText(context, "Sticker file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val extension = if (isAnimated) "gif" else "webp"
        val mimeType = if (isAnimated) "image/gif" else "image/webp"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Sticker_${System.currentTimeMillis()}.$extension")
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SilverSticker")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                srcFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Toast.makeText(context, "Saved as $extension to Pictures/SilverSticker", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to download sticker", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("DownloadSticker", "Failed to download sticker: ${e.message}", e)
        Toast.makeText(context, "Failed to download sticker", Toast.LENGTH_SHORT).show()
    }
}

private fun shareSticker(context: Context, sticker: Sticker) {
    try {
        val webpFile = sticker.getInternalFile(context)
        val gifFile = java.io.File(webpFile.parentFile, sticker.imageFileName.replace(".webp", ".gif"))
        val isAnimated = sticker.isAnimated && gifFile.exists()
        val srcFile = if (isAnimated) gifFile else webpFile

        if (!srcFile.exists()) {
            Toast.makeText(context, "Sticker file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", srcFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (isAnimated) "image/gif" else "image/webp"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Sticker"))
    } catch (e: Exception) {
        Log.e("ShareSticker", "Failed to share sticker: ${e.message}", e)
        Toast.makeText(context, "Failed to share sticker", Toast.LENGTH_SHORT).show()
    }
}
private fun shareMultipleStickers(context: Context, stickers: List<Sticker>) {
    try {
        val uris = ArrayList<Uri>()
        var hasAnimated = false
        stickers.forEach { sticker ->
            val webpFile = sticker.getInternalFile(context)
            val gifFile = java.io.File(webpFile.parentFile, sticker.imageFileName.replace(".webp", ".gif"))
            val isAnimated = sticker.isAnimated && gifFile.exists()
            if (isAnimated) hasAnimated = true
            val srcFile = if (isAnimated) gifFile else webpFile
            if (srcFile.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", srcFile)
                uris.add(uri)
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(context, "No shareable stickers found", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = if (hasAnimated) "image/*" else "image/webp"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Stickers"))
    } catch (e: Exception) {
        Log.e("ShareMultipleStickers", "Failed to share stickers: ${e.message}", e)
        Toast.makeText(context, "Failed to share stickers", Toast.LENGTH_SHORT).show()
    }
}
