package com.f8scape.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.f8scape.app.theme.*
import com.f8scape.cards.CardItem
import com.f8scape.cards.DefaultCardLayouts
import com.f8scape.data.PreferencesRepository
import com.f8scape.data.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StandbyDashboard(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepo = remember { PreferencesRepository(context) }
    val preferences by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isEditMode by remember { mutableStateOf(false) }

    // Cards state loaded from DataStore preferences
    val currentCards = remember(preferences.customLayoutSerialized, preferences.activePreset) {
        DefaultCardLayouts.deserialize(
            serialized = preferences.customLayoutSerialized,
            fallbackPreset = preferences.activePreset
        )
    }
    var localCards by remember(currentCards) { mutableStateOf(currentCards) }

    // Periodic minute time tick
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            val millisUntilNextMinute = 60000L - (System.currentTimeMillis() % 60000L)
            delay(millisUntilNextMinute.coerceAtLeast(1000L))
        }
    }

    // OLED Burn-in mitigation: subtle microscopic shift (1-2px) every 2 minutes
    val burnInShiftX by remember(currentTimeMs / 120000L) {
        mutableIntStateOf(((currentTimeMs / 120000L) % 3 - 1).toInt())
    }
    val burnInShiftY by remember(currentTimeMs / 120000L) {
        mutableIntStateOf((((currentTimeMs / 120000L) + 1) % 3 - 1).toInt())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(18.dp)
            .offset { IntOffset(burnInShiftX, burnInShiftY) }
    ) {
        // Exit tap target (subtle X button) in top right
        if (!isEditMode) {
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .alpha(0.6f)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close StandBy", tint = TextTertiary)
            }
        }

        // Snap-to-Grid Card Engine
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isEditMode) 0.dp else 24.dp, bottom = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            DashboardGridEngine(
                cards = localCards,
                currentTimeMs = currentTimeMs,
                weatherCity = preferences.weatherCity,
                maskCalendar = preferences.calendarMaskLocked,
                isEditMode = isEditMode,
                onEnterEditMode = { isEditMode = true },
                onUpdateCards = { updated ->
                    localCards = updated
                    coroutineScope.launch {
                        preferencesRepo.setCustomLayout(DefaultCardLayouts.serialize(updated))
                    }
                },
                onDoneEdit = { isEditMode = false },
                onResetDefaults = {
                    val defaultList = when (preferences.activePreset) {
                        "Minimal" -> DefaultCardLayouts.getMinimalLayout()
                        "Media" -> DefaultCardLayouts.getMediaLayout()
                        else -> DefaultCardLayouts.getDashboardLayout()
                    }
                    localCards = defaultList
                    coroutineScope.launch {
                        preferencesRepo.setCustomLayout("")
                    }
                }
            )
        }

        // Bottom ambient hint
        if (!isEditMode) {
            Text(
                text = "F8scape · Long press to customize · Double tap to exit",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .alpha(0.45f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )
        }

        // Ambient dimming scrim
        if (preferences.screenDimLevel < 0.8f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = (1.0f - preferences.screenDimLevel) * 0.45f))
            )
        }
    }
}
