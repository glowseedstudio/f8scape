package com.f8scape.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.cards.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardGridEngine(
    cards: List<CardItem>,
    currentTimeMs: Long,
    weatherCity: String = "Canberra",
    maskCalendar: Boolean = false,
    isEditMode: Boolean = false,
    onEnterEditMode: () -> Unit = {},
    onUpdateCards: (List<CardItem>) -> Unit = {},
    onDoneEdit: () -> Unit = {},
    onResetDefaults: () -> Unit = {}
) {
    var showAddCardDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClick = onEnterEditMode
            )
    ) {
        val isLandscape = maxWidth > 550.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Edit mode toolbar
            if (isEditMode) {
                Surface(
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOMIZE GRID",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Add Card Button
                            IconButton(
                                onClick = { showAddCardDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add card", tint = AccentBlue)
                            }

                            // Reset Button
                            IconButton(
                                onClick = onResetDefaults,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = TextSecondary)
                            }

                            // Done Button
                            Button(
                                onClick = onDoneEdit,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PureBlack, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Done", color = PureBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Cards composition
            if (isLandscape) {
                // Fold 8 Landscape Split: Left has Clock (if present); Right has stacked companion cards
                val clockCard = cards.find { it.type == CardType.CLOCK }
                val companionCards = cards.filter { it.type != CardType.CLOCK }

                if (clockCard != null && companionCards.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Pane: Prominent Clock
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            AmbientClockCard(
                                currentTimeMs = currentTimeMs,
                                isLarge = true,
                                isEditMode = isEditMode,
                                onRemove = if (cards.size > 1) {
                                    { onUpdateCards(cards.filter { it.id != clockCard.id }) }
                                } else null
                            )
                        }

                        // Right Pane: Native Companion Cards (Weather, Battery, Calendar)
                        LazyColumn(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            itemsIndexed(companionCards) { index, item ->
                                RenderCard(
                                    item = item,
                                    currentTimeMs = currentTimeMs,
                                    weatherCity = weatherCity,
                                    maskCalendar = maskCalendar,
                                    isEditMode = isEditMode,
                                    onMoveUp = if (index > 0) {
                                        {
                                            val mutable = companionCards.toMutableList()
                                            val prev = mutable.removeAt(index - 1)
                                            mutable.add(index, prev)
                                            onUpdateCards(listOf(clockCard) + mutable)
                                        }
                                    } else null,
                                    onMoveDown = if (index < companionCards.size - 1) {
                                        {
                                            val mutable = companionCards.toMutableList()
                                            val current = mutable.removeAt(index)
                                            mutable.add(index + 1, current)
                                            onUpdateCards(listOf(clockCard) + mutable)
                                        }
                                    } else null,
                                    onRemove = {
                                        onUpdateCards(cards.filter { it.id != item.id })
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Minimal mode: Centered layout with generous negative space
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(cards) { index, item ->
                            Box(modifier = Modifier.fillMaxWidth(0.85f).padding(vertical = 8.dp)) {
                                RenderCard(
                                    item = item,
                                    currentTimeMs = currentTimeMs,
                                    weatherCity = weatherCity,
                                    maskCalendar = maskCalendar,
                                    isEditMode = isEditMode,
                                    onRemove = if (cards.size > 1) {
                                        { onUpdateCards(cards.filter { it.id != item.id }) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            } else {
                // Portrait stack
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(cards) { index, item ->
                        RenderCard(
                            item = item,
                            currentTimeMs = currentTimeMs,
                            weatherCity = weatherCity,
                            maskCalendar = maskCalendar,
                            isEditMode = isEditMode,
                            onMoveUp = if (index > 0) {
                                {
                                    val mutable = cards.toMutableList()
                                    val prev = mutable.removeAt(index - 1)
                                    mutable.add(index, prev)
                                    onUpdateCards(mutable)
                                }
                            } else null,
                            onMoveDown = if (index < cards.size - 1) {
                                {
                                    val mutable = cards.toMutableList()
                                    val current = mutable.removeAt(index)
                                    mutable.add(index + 1, current)
                                    onUpdateCards(mutable)
                                }
                            } else null,
                            onRemove = if (cards.size > 1) {
                                { onUpdateCards(cards.filter { it.id != item.id }) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Add Card Dialog
    if (showAddCardDialog) {
        val existingTypes = cards.map { it.type }.toSet()
        val availableTypes = CardType.values().filter { it != CardType.SPACER && !existingTypes.contains(it) }

        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("Add Card to Grid", color = TextPrimary) },
            text = {
                if (availableTypes.isEmpty()) {
                    Text("All available cards are already added to your dashboard.", color = TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableTypes.forEach { type ->
                            Surface(
                                onClick = {
                                    val newId = "${type.name.lowercase()}_${System.currentTimeMillis()}"
                                    onUpdateCards(cards + CardItem(id = newId, type = type))
                                    showAddCardDialog = false
                                },
                                color = SurfaceVariantDark,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = type.displayName,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("Cancel", color = AccentBlue)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun RenderCard(
    item: CardItem,
    currentTimeMs: Long,
    weatherCity: String,
    maskCalendar: Boolean,
    isEditMode: Boolean,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    when (item.type) {
        CardType.CLOCK -> AmbientClockCard(
            currentTimeMs = currentTimeMs,
            isLarge = false,
            isEditMode = isEditMode,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove
        )
        CardType.WEATHER -> AmbientWeatherCard(
            cityName = weatherCity,
            isEditMode = isEditMode,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove
        )
        CardType.BATTERY -> AmbientBatteryCard(
            isEditMode = isEditMode,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove
        )
        CardType.CALENDAR -> AmbientCalendarCard(
            maskTitle = maskCalendar,
            isEditMode = isEditMode,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove
        )
        CardType.SPACER -> Spacer(modifier = Modifier.height(16.dp))
    }
}
