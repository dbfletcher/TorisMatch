package com.example.torismatch

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset // <--- THIS WAS THE MISSING LINE
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF1F8E9)) {
                MatchGame()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchGame() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ToriMatchPrefs", Context.MODE_PRIVATE) }

    // --- Emoticon Library ---
    val allEmoticons = remember {
        listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🐒", "🦍", "🦧", "🐕", "🦮", "🐕‍🦺", "🐩", "🐺", "🦝", "🐈", "🐈‍⬛", "🐅", "🐆", "🐴", "🐎", "🦄", "🦓", "🦌", "🦬", "🐂", "🐃", "🐄", "🐖", "🐗", "🐏", "🐑", "🐐", "🐪", "🐫", "🦙", "🦒", "🐘", "🦣", "🦏", "🦛", "🐁", "🐀", "🐇", "🐿️", "🦫", "🦔", "🦇", "🐻‍❄️", "🦥", "🦦", "🦨", "🦘", "🦡",
            "🦃", "🐔", "🐓", "🐣", "🐤", "🐥", "🐦", "🐧", "🕊️", "🦅", "🦆", "🦢", "🦉", "🦩", "🦚", "🦜", "🪿", "🐦‍⬛", "🐦‍🔥",
            "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🐊",
            "🐌", "🦋", "🐛", "🐜", "🐝", "🪲", "🐞", "🦗", "🕷️", "🕸️", "🦂", "🦟", "🪰", "🪱", "🪳"
        )
    }

    var difficulty by remember { mutableStateOf("Easy") }
    var gameId by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    var bestTime by remember(difficulty, gameId) {
        mutableStateOf(prefs.getInt("BestTimeMatch_$difficulty", 0))
    }

    // --- GRID LOGIC ---
    val pairCount = when(difficulty) {
        "Easy" -> 8    // 4x4 grid (16 cards)
        "Medium" -> 18 // 6x6 grid (36 cards)
        else -> 32     // 8x8 grid (64 cards)
    }

    val columnCount = when(difficulty) {
        "Easy" -> 4
        "Medium" -> 6
        else -> 8
    }

    // Dynamic Text Size for tighter grids
    val emojiSize = when(difficulty) {
        "Easy" -> 32.sp
        "Medium" -> 24.sp
        else -> 16.sp
    }

    val cards = remember(gameId, difficulty) {
        val selected = allEmoticons.shuffled().take(pairCount)
        (selected + selected).shuffled()
    }

    var flippedIndices by remember { mutableStateOf(setOf<Int>()) }
    var matchedIndices by remember { mutableStateOf(setOf<Int>()) }
    var hasWon by remember { mutableStateOf(false) }

    // --- Sound Setup ---
    val soundPool = remember {
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build()
    }
    val winSoundId = remember {
        val resId = context.resources.getIdentifier("win_sound", "raw", context.packageName)
        if (resId != 0) soundPool.load(context, resId, 1) else -1
    }
    var winStreamId by remember { mutableStateOf(0) }

    // Timer Logic
    LaunchedEffect(gameId, hasWon) {
        if (!hasWon) {
            seconds = 0
            if (winStreamId != 0) { soundPool.stop(winStreamId); winStreamId = 0 }
            while (!hasWon) { delay(1000); seconds++ }
        }
    }

    // Victory Logic
    LaunchedEffect(hasWon) {
        if (hasWon) {
            if (winSoundId != -1) winStreamId = soundPool.play(winSoundId, 1f, 1f, 1, -1, 1f)
            val currentBest = prefs.getInt("BestTimeMatch_$difficulty", 0)
            if (currentBest == 0 || seconds < currentBest) {
                prefs.edit().putInt("BestTimeMatch_$difficulty", seconds).apply()
                bestTime = seconds
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text("Tori's Match", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))

            // Difficulty Buttons
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("Easy", "Medium", "Hard").forEach { level ->
                    Button(
                        onClick = {
                            difficulty = level; gameId++; hasWon = false
                            flippedIndices = emptySet(); matchedIndices = emptySet()
                        },
                        modifier = Modifier.padding(2.dp).weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (difficulty == level) Color(0xFF388E3C) else Color.LightGray),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(level, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }

            // Game Grid
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    contentPadding = PaddingValues(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(cards) { index, emoji ->
                        val isFlipped = flippedIndices.contains(index) || matchedIndices.contains(index)

                        Card(
                            modifier = Modifier
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clickable(!isFlipped && flippedIndices.size < 2) {
                                    flippedIndices = flippedIndices + index
                                    if (flippedIndices.size == 2) {
                                        val list = flippedIndices.toList()
                                        if (cards[list[0]] == cards[list[1]]) {
                                            matchedIndices = matchedIndices + flippedIndices
                                            flippedIndices = emptySet()
                                            if (matchedIndices.size == cards.size) hasWon = true
                                        } else {
                                            // Delay handled by LaunchedEffect below
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFlipped) Color.White else Color(0xFF1E88E5)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (isFlipped) {
                                    Text(emoji, fontSize = emojiSize)
                                } else {
                                    Text("❓", fontSize = emojiSize, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Auto-flip back logic
            LaunchedEffect(flippedIndices) {
                if (flippedIndices.size == 2) {
                    val list = flippedIndices.toList()
                    if (cards[list[0]] != cards[list[1]]) {
                        delay(1000)
                        flippedIndices = emptySet()
                    }
                }
            }

            // Stats Bar
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("⏱️ ${formatTime(seconds)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = if (bestTime == 0) "Best: --:--" else "🏆 Best: ${formatTime(bestTime)}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { prefs.edit().remove("BestTimeMatch_$difficulty").apply(); gameId++ }))
                    Text("(Long press reset)", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(48.dp)) // S25 Nav Bar Buffer
        }

        // --- Victory Overlay ---
        if (hasWon) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
            RealFireworks()
            AlertDialog(
                onDismissRequest = { },
                confirmButton = { Button(onClick = { if (winStreamId != 0) soundPool.stop(winStreamId); gameId++; hasWon = false; flippedIndices = emptySet(); matchedIndices = emptySet() }) { Text("Play Again!") } },
                title = { Text("🌟 YOU DID IT! 🌟") },
                text = { Text("Time: ${formatTime(seconds)}\nBest for $difficulty: ${formatTime(bestTime)}") }
            )
        }
    }
}

fun formatTime(totalSeconds: Int): String {
    val mins = totalSeconds / 60; val secs = totalSeconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
fun RealFireworks() {
    val infiniteTransition = rememberInfiniteTransition()
    val colors = listOf(Color(0xFFFFFF00), Color(0xFFFF0000), Color(0xFF00FFFF), Color(0xFF00FF00), Color(0xFFFF00FF), Color(0xFFFF9800))
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(6) { burstIndex ->
            val startX = remember { Random.nextFloat() }; val startY = remember { Random.nextFloat() * 0.4f + 0.1f }
            val burstColor = remember { colors.random() }; val delayMillis = remember { (burstIndex * 500) }
            val progress by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(2200, delayMillis = delayMillis, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(startX * size.width, startY * size.height)
                for (i in 0 until 20) {
                    val angle = (i * 18.0) * (Math.PI / 180.0); val radius = progress * 500f; val gravity = progress * progress * 350f
                    val sparkX = center.x + (radius * Math.cos(angle)).toFloat(); val sparkY = center.y + (radius * Math.sin(angle)).toFloat() + gravity
                    drawCircle(color = Color.White.copy(alpha = 1f - progress), radius = 4f, center = Offset(sparkX, sparkY))
                    drawCircle(color = burstColor.copy(alpha = 1f - (progress * 0.8f)), radius = 12f, center = Offset(sparkX, sparkY))
                }
            }
        }
    }
}