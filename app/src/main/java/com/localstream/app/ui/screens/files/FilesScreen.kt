package com.localstream.app.ui.screens.files

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localstream.app.LocalStreamApplication
import com.localstream.app.domain.model.MediaFile
import com.localstream.app.domain.model.MediaType
import com.localstream.app.ui.components.MediaFileCard
import com.localstream.app.ui.theme.BorderHighlight
import com.localstream.app.ui.theme.BorderSubtle
import com.localstream.app.ui.theme.CyanAccent
import com.localstream.app.ui.theme.DarkBgBase
import com.localstream.app.ui.theme.DarkBgCard
import com.localstream.app.ui.theme.DarkBgSurface
import com.localstream.app.ui.theme.EmeraldSuccess
import com.localstream.app.ui.theme.PurpleNeon
import com.localstream.app.ui.theme.PurplePrimary
import com.localstream.app.ui.theme.PurpleSecondary
import com.localstream.app.ui.theme.TextMuted
import com.localstream.app.ui.theme.TextPrimary
import com.localstream.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class SortMode {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    SIZE_DESC
}

@Composable
fun FilesScreen(
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as LocalStreamApplication
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<MediaType?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val allFiles by app.fileRepository.allFilesFlow.collectAsState(initial = emptyList())
    val allFolders by app.fileRepository.getAllFolders().collectAsState(initial = emptyList())

    // SAF Directory picker launcher
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            scope.launch {
                app.settingsRepository.addSelectedFolder(uri.toString())
                app.fileScanner.scanAllMedia(setOf(uri.toString()))
                Toast.makeText(context, "Added folder to library", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Filter and sort files
    val filteredFiles = remember(allFiles, searchQuery, selectedCategory, selectedFolder, sortMode) {
        var list = allFiles

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.folder.contains(searchQuery, ignoreCase = true)
            }
        }

        if (selectedCategory != null) {
            list = list.filter { it.mediaType == selectedCategory }
        }

        if (selectedFolder != null) {
            list = list.filter { it.folder == selectedFolder }
        }

        when (sortMode) {
            SortMode.DATE_DESC -> list.sortedByDescending { it.modifiedDate }
            SortMode.DATE_ASC -> list.sortedBy { it.modifiedDate }
            SortMode.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortMode.SIZE_DESC -> list.sortedByDescending { it.size }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBgBase,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { folderPicker.launch(null) },
                containerColor = PurpleSecondary,
                contentColor = TextPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_folder_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Shared Folder"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Media Explorer",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredFiles.size} items available",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Queue All button
                        if (filteredFiles.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    app.queueManager.setQueue(filteredFiles)
                                    Toast.makeText(context, "Added ${filteredFiles.size} to queue", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("queue_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Queue All",
                                    tint = PurplePrimary
                                )
                            }
                        }

                        // Sort Menu
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Files",
                                    tint = TextSecondary
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(DarkBgCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Newest first", color = TextPrimary) },
                                    onClick = { sortMode = SortMode.DATE_DESC; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Oldest first", color = TextPrimary) },
                                    onClick = { sortMode = SortMode.DATE_ASC; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A-Z)", color = TextPrimary) },
                                    onClick = { sortMode = SortMode.NAME_ASC; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Largest size", color = TextPrimary) },
                                    onClick = { sortMode = SortMode.SIZE_DESC; showSortMenu = false }
                                )
                            }
                        }

                        // Rescan Button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    app.fileScanner.scanAllMedia()
                                    Toast.makeText(context, "Media library refreshed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("refresh_files_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("files_search_input"),
                    placeholder = { Text("Search files, titles, extensions...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PurplePrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBgSurface,
                        unfocusedContainerColor = DarkBgSurface,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            // Category Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            label = "All Media",
                            isSelected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                    }
                    item {
                        FilterChip(
                            label = "🎬 Videos",
                            isSelected = selectedCategory == MediaType.VIDEO,
                            onClick = { selectedCategory = MediaType.VIDEO }
                        )
                    }
                    item {
                        FilterChip(
                            label = "🎵 Music",
                            isSelected = selectedCategory == MediaType.AUDIO,
                            onClick = { selectedCategory = MediaType.AUDIO }
                        )
                    }
                    item {
                        FilterChip(
                            label = "🖼️ Photos",
                            isSelected = selectedCategory == MediaType.IMAGE,
                            onClick = { selectedCategory = MediaType.IMAGE }
                        )
                    }
                    item {
                        FilterChip(
                            label = "📄 Documents",
                            isSelected = selectedCategory == MediaType.DOCUMENT,
                            onClick = { selectedCategory = MediaType.DOCUMENT }
                        )
                    }
                }
            }

            // Folder Filter Row (if folders exist)
            if (allFolders.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FolderChip(
                                label = "All Folders",
                                isSelected = selectedFolder == null,
                                onClick = { selectedFolder = null }
                            )
                        }
                        items(allFolders) { folderName ->
                            FolderChip(
                                label = folderName,
                                isSelected = selectedFolder == folderName,
                                onClick = { selectedFolder = if (selectedFolder == folderName) null else folderName }
                            )
                        }
                    }
                }
            }

            // Empty state or file list
            if (filteredFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkBgSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No media files matching criteria",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Use '+' to pick a folder or pull to refresh.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredFiles, key = { it.id }) { file ->
                    MediaFileCard(
                        file = file,
                        onPlay = {
                            app.playerManager.playMedia(file)
                            onNavigateToPlayer()
                        },
                        onAddToQueue = {
                            app.queueManager.addToQueue(file)
                            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                        },
                        onShare = {
                            app.storageManager.shareMediaFile(file)
                        },
                        onDelete = {
                            scope.launch {
                                app.storageManager.deleteMediaFile(file)
                                app.fileRepository.deleteFile(file)
                                Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PurplePrimary else DarkBgSurface)
            .border(1.dp, if (isSelected) PurplePrimary else BorderSubtle, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) DarkBgBase else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FolderChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PurpleSecondary.copy(alpha = 0.25f) else DarkBgCard)
            .border(1.dp, if (isSelected) PurplePrimary else BorderSubtle, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (isSelected) PurplePrimary else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
