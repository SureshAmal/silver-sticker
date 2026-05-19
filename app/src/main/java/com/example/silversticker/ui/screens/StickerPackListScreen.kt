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
import coil.compose.AsyncImage
import com.example.silversticker.models.StickerPack
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
                    enter = fadeIn(tween(600)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val infiniteTransition = rememberInfiniteTransition(label = "empty")
                        val floatOffset by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = FastOutSlowInEasing),
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
                        index = index,
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
    index: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    val isInitialItems = index < 8
    var hasAnimated by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val animAlpha = remember { Animatable(if (isInitialItems && !hasAnimated) 0f else 1f) }
    val animTranslationY = remember { Animatable(if (isInitialItems && !hasAnimated) 80f else 0f) }

    LaunchedEffect(pack.identifier) {
        if (isInitialItems && !hasAnimated) {
            val delayTime = index * 60L
            delay(delayTime)
            launch {
                animAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
            launch {
                animTranslationY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }.join()
            hasAnimated = true
        }
    }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    // Press animation state
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pack_scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 8f else 1f,
        animationSpec = tween(150),
        label = "pack_elevation"
    )

    AnimatedVisibility(
        visible = !isDeleting,
        exit = fadeOut(tween(300)) +
                scaleOut(
                    targetScale = 0.5f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) +
                shrinkVertically(animationSpec = tween(350))
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = animAlpha.value
                    translationY = animTranslationY.value
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
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val trayFile = pack.getTrayInternalFile(context)
                AsyncImage(
                    model = if (trayFile.exists()) trayFile else null,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Fit
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
