package com.f8scape.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    showWeather: Boolean = true,
    showBattery: Boolean = true,
    maskCalendar: Boolean = false,
    isEditMode: Boolean = false,
    onEnterEditMode: () -> Unit = {},
    onUpdateCards: (List<CardItem>) -> Unit = {},
    onDoneEdit: () -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onDoubleTap: (() -> Unit)? = null,
    onExpandClock: (() -> Unit)? = null,
    onExpandMusic: (() -> Unit)? = null,
    onExpandMonthCalendar: (() -> Unit)? = null,
    onSelectCalendarDay: ((year: Int, month: Int, day: Int) -> Unit)? = null
) {
    var showAddCardDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onDoubleClick = onDoubleTap,
                onLongClick = onEnterEditMode
            )
    ) {
        val isLandscape = maxWidth > 550.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Edit mode toolbar: Frosted Samsung One UI aesthetic
            if (isEditMode) {
                Surface(
                    color = Color(0xCC18181A),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOMIZE GRID",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Add Card Button
                            IconButton(
                                onClick = { showAddCardDialog = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add card", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            // Reset Button
                            IconButton(
                                onClick = onResetDefaults,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                            }

                            // Done Button: Samsung One UI style pure white pill with black text
                            Button(
                                onClick = onDoneEdit,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Done", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                weatherCity = weatherCity,
                                showWeather = showWeather,
                                showBattery = showBattery,
                                isLarge = true,
                                isEditMode = isEditMode,
                                onClockClick = { onExpandClock?.invoke() },
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
                                    showWeather = showWeather,
                                    showBattery = showBattery,
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
                                    },
                                    onExpandClock = onExpandClock,
                                    onExpandMusic = onExpandMusic,
                                    onExpandMonthCalendar = onExpandMonthCalendar,
                                    onSelectCalendarDay = onSelectCalendarDay
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
                                    showWeather = showWeather,
                                    showBattery = showBattery,
                                    maskCalendar = maskCalendar,
                                    isEditMode = isEditMode,
                                    onRemove = if (cards.size > 1) {
                                        { onUpdateCards(cards.filter { it.id != item.id }) }
                                    } else null,
                                    onExpandClock = onExpandClock,
                                    onExpandMusic = onExpandMusic,
                                    onExpandMonthCalendar = onExpandMonthCalendar,
                                    onSelectCalendarDay = onSelectCalendarDay
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
                            showWeather = showWeather,
                            showBattery = showBattery,
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
                            } else null,
                            onExpandClock = onExpandClock,
                            onExpandMusic = onExpandMusic,
                            onExpandMonthCalendar = onExpandMonthCalendar,
                            onSelectCalendarDay = onSelectCalendarDay
                        )
                    }
                }
            }
        }

        // Inline Add Card Sheet
        if (showAddCardDialog) {
            val existingTypes = cards.map { it.type }.toSet()
            val allTypes = listOf(CardType.CLOCK, CardType.MEDIA, CardType.CALENDAR)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showAddCardDialog = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xF518181A),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .fillMaxWidth(if (isLandscape) 0.65f else 0.90f)
                        .padding(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add Card to Grid",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { showAddCardDialog = false },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                            }
                        }

                        Text(
                            text = "Tap a card to add or restore it on your dashboard:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.60f)
                        )

                        allTypes.forEach { type ->
                            val isAdded = existingTypes.contains(type)
                            Surface(
                                onClick = {
                                    val newId = "${type.name.lowercase()}_${System.currentTimeMillis()}"
                                    onUpdateCards(cards + CardItem(id = newId, type = type))
                                    showAddCardDialog = false
                                },
                                color = if (isAdded) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = if (isAdded) 0.08f else 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = type.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = type.defaultTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.60f)
                                        )
                                    }

                                    if (isAdded) {
                                        Text(
                                            text = "On Screen (+)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.60f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color.White.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
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

@Composable
private fun RenderCard(
    item: CardItem,
    currentTimeMs: Long,
    weatherCity: String,
    showWeather: Boolean = true,
    showBattery: Boolean = true,
    maskCalendar: Boolean,
    isEditMode: Boolean,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onExpandClock: (() -> Unit)? = null,
    onExpandMusic: (() -> Unit)? = null,
    onExpandMonthCalendar: (() -> Unit)? = null,
    onSelectCalendarDay: ((year: Int, month: Int, day: Int) -> Unit)? = null
) {
    when (item.type) {
        CardType.CLOCK -> AmbientClockCard(
            currentTimeMs = currentTimeMs,
            weatherCity = weatherCity,
            showWeather = showWeather,
            showBattery = showBattery,
            isLarge = false,
            isEditMode = isEditMode,
            onClockClick = { onExpandClock?.invoke() },
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
            onDayClick = { year, month, day ->
                onSelectCalendarDay?.invoke(year, month, day)
            },
            onMonthClick = {
                onExpandMonthCalendar?.invoke()
            },
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove
        )
        CardType.MEDIA -> AmbientMediaCard(
            isEditMode = isEditMode,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove,
            onExpandMusic = onExpandMusic
        )
        CardType.SPACER -> Spacer(modifier = Modifier.height(16.dp))
    }
}
