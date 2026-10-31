package com.f8scape.photoframe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.data.PreferencesRepository
import com.f8scape.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ScreenBg = Color(0xFF0A0A0C)
private val CardBg = Color(0xFF161618)

/**
 * Native Compose screen for managing the Photo Frame collection:
 * - Select multiple photographs via system Photo Picker
 * - Select folder via Storage Access Framework
 * - Remove individual photos or clear collection
 * - Shuffle toggle
 * - Thumbnail preview grid
 * - Full-screen slideshow preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoFrameCollectionScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit,
    onPreviewSlideshow: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val sourceRepo = remember { PhotoFrameSourceRepository(context) }

    var resolvedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoadingUris by remember { mutableStateOf(true) }

    LaunchedEffect(
        preferences.photoFrameSourceType,
        preferences.photoFrameSelectedUrisSerialized,
        preferences.photoFrameFolderUri
    ) {
        isLoadingUris = true
        resolvedUris = sourceRepo.resolveActiveUris(
            sourceType = preferences.photoFrameSourceType,
            selectedUrisSerialized = preferences.photoFrameSelectedUrisSerialized,
            folderUriString = preferences.photoFrameFolderUri
        )
        isLoadingUris = false
    }

    // System Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri -> sourceRepo.takePersistablePermission(uri) }
            coroutineScope.launch {
                preferencesRepository.setPhotoFrameSourceType("PHOTOS")
                preferencesRepository.addPhotoFrameUris(uris.map { it.toString() })
            }
        }
    }

    // Storage Access Framework folder launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            sourceRepo.takePersistablePermission(treeUri)
            coroutineScope.launch {
                preferencesRepository.setPhotoFrameSourceType("FOLDER")
                preferencesRepository.setPhotoFrameFolderUri(treeUri.toString())
            }
        }
    }

    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Photo Collection",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val sourceText = if (preferences.photoFrameSourceType == "FOLDER") {
                            "Folder: ${sourceRepo.getFolderDisplayName(preferences.photoFrameFolderUri)}"
                        } else {
                            "Selected Photos (${resolvedUris.size})"
                        }
                        Text(
                            text = sourceText,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (resolvedUris.isNotEmpty()) {
                        IconButton(onClick = onPreviewSlideshow) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Preview Slideshow",
                                tint = SamsungBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScreenBg
                )
            )
        },
        containerColor = ScreenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
        ) {
            // Source Switch & Actions Header Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = CardBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Source selection buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (preferences.photoFrameSourceType == "PHOTOS") SamsungBlue else Color(0xFF26262A)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Photos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                folderPickerLauncher.launch(null)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (preferences.photoFrameSourceType == "FOLDER") SamsungBlue else Color(0xFF26262A)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Folder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                    // Shuffle toggle and Clear Collection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Text("Shuffle Playback", fontSize = 14.sp, color = Color.White)
                        }
                        Switch(
                            checked = preferences.photoFrameShuffle,
                            onCheckedChange = { shuffle ->
                                coroutineScope.launch { preferencesRepository.setPhotoFrameShuffle(shuffle) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SamsungBlue,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }

                    if (resolvedUris.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${resolvedUris.size} photos ready",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )

                            TextButton(
                                onClick = { showClearConfirmation = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Collection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Thumbnail Grid or Empty State
            if (isLoadingUris) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SamsungBlue,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (resolvedUris.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Photos in Collection",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Tap 'Add Photos' to select pictures using Android Photo Picker, or 'Choose Folder' to pick a folder from your storage.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 380.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 12.dp)
                ) {
                    items(resolvedUris, key = { it.toString() }) { uri ->
                        PhotoThumbnailItem(
                            uri = uri,
                            canRemove = preferences.photoFrameSourceType == "PHOTOS",
                            onRemove = {
                                sourceRepo.releasePersistablePermission(uri)
                                coroutineScope.launch {
                                    preferencesRepository.removePhotoFrameUri(uri.toString())
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear Photo Collection?") },
            text = { Text("This will remove all selected photos and folder selections from Photo Frame. Your original files will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        coroutineScope.launch {
                            preferencesRepository.clearPhotoFrameCollection()
                        }
                    }
                ) {
                    Text("Clear", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = CardBg,
            titleContentColor = Color.White,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun PhotoThumbnailItem(
    uri: Uri,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            val thumb = PhotoDecoder.decodeThumbnail(context, uri)
            bitmap = thumb
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E1E22))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SamsungBlue.copy(alpha = 0.5f),
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (canRemove) {
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = PureBlack.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove photo",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
