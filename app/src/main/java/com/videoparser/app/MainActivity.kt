package com.videoparser.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.toastEvents.collectLatest { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            LaunchedEffect(Unit) {
                viewModel.copyEvents.collectLatest { text ->
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("link", text))
                }
            }
            LaunchedEffect(Unit) {
                viewModel.shareEvents.collectLatest { data ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, data.title.ifBlank { "分享视频" })
                        putExtra(Intent.EXTRA_TEXT, data.videoUrl)
                    }
                    startActivity(Intent.createChooser(intent, "分享视频"))
                }
            }
            VideoParserScreen(viewModel)
        }
    }
}

/* ═══════════════════════ iOS Design System - Colors ═══════════════════════ */

// Primary - iOS System Blue
private val iOSBlue       = Color(0xFF007AFF)
private val iOSBlueLight  = Color(0xFF5AC8FA)
private val iOSBlueDark   = Color(0xFF0055CC)

// Semantic
private val iOSGreen      = Color(0xFF34C759)
private val iOSGreenLight = Color(0xFF30D158)
private val iOSOrange     = Color(0xFFFF9500)
private val iOSRed        = Color(0xFFFF3B30)
private val iOSTeal       = Color(0xFF5AC8FA)
private val iOSPurple     = Color(0xFFAF52DE)

// Neutrals
private val iOSBgBase     = Color(0xFFF2F2F7)
private val iOSCardBg     = Color(0xFFFFFFFF)
private val iOSLabel      = Color(0xFF1C1C1E)
private val iOSSecondary  = Color(0xFF3C3C43).copy(alpha = 0.6f)
private val iOSTertiary   = Color(0xFF3C3C43).copy(alpha = 0.3f)
private val iOSPlaceholder = Color(0xFF3C3C43).copy(alpha = 0.3f)
private val iOSSeparator  = Color(0xFF3C3C43).copy(alpha = 0.15f)

// Glass
private val GlassWhite    = Color(0xFFFFFFFF).copy(alpha = 0.78f)
private val GlassBorder   = Color(0xFFFFFFFF).copy(alpha = 0.55f)
private val GlassShadow   = Color(0xFF000000).copy(alpha = 0.04f)

// Gradients
private val BgGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFF9F9FC),
        Color(0xFFF2F2F7),
        Color(0xFFEDEDF5),
        Color(0xFFF5F5FA)
    )
)
private val AccentGradient = Brush.horizontalGradient(
    listOf(iOSBlue, Color(0xFF5856D6))
)
private val AccentGradientDisabled = Brush.horizontalGradient(
    listOf(Color(0xFFB0C4DE), Color(0xFFC5B8E0))
)
private val GreenGradient = Brush.linearGradient(
    listOf(iOSGreen, iOSGreenLight)
)
private val TealGradient  = Brush.linearGradient(
    listOf(iOSTeal, iOSBlue)
)
private val OrangeGradient = Brush.linearGradient(
    listOf(iOSOrange, Color(0xFFFF648E))
)
private val PurpleGradient = Brush.linearGradient(
    listOf(iOSPurple, Color(0xFF5856D6))
)

/* ═══════════════════════ Responsive Helpers ═══════════════════════════════ */

@Composable
private fun responsivePadding(): Dp {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp
    return when {
        widthDp >= 600 -> 48.dp
        widthDp >= 400 -> 24.dp
        else -> 16.dp
    }
}

@Composable
private fun cardMaxWidth(): Dp {
    val config = LocalConfiguration.current
    return if (config.screenWidthDp >= 600) 560.dp else Dp.Unspecified
}

/* ═════════════════════════ Screen ═════════════════════════════════════════ */

@Composable
fun VideoParserScreen(vm: MainViewModel) {
    val pad = responsivePadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pad, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header ──
            Text(
                text = "视频解析助手",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = iOSLabel,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(20.dp))

            // Input card
            InputCard(
                videoUrl = vm.videoUrl,
                onUrlChanged = vm::onUrlChanged
            )

            Spacer(Modifier.height(16.dp))

            // Button row
            ButtonRow(
                isParsing = vm.isParsing,
                hasUrl = vm.videoUrl.isNotBlank(),
                onPaste = {
                    val ctx = it
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
                    vm.onPasted(text)
                },
                onParse = vm::parse
            )

            Spacer(Modifier.height(24.dp))

            // Result or empty state
            val data = vm.parseData
            if (data != null) {
                ResultSection(
                    data = data,
                    isDownloading = vm.isDownloading,
                    downloadPercent = vm.downloadPercent,
                    isGettingShortLink = vm.isGettingShortLink,
                    onDownload = vm::download,
                    onCopyLongLink = vm::copyLongLink,
                    onCopyShortLink = vm::copyShortLink,
                    onShare = vm::share
                )
            } else {
                EmptyState()
            }

            Spacer(Modifier.height(32.dp))
        }

        // Global loading overlay
        AnimatedVisibility(
            visible = vm.isParsing,
            enter = fadeIn(spring()),
            exit = fadeOut(spring())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(iOSLabel.copy(alpha = 0.35f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                GlassCardCompact {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp)
                    ) {
                        CircularProgressIndicator(
                            color = iOSBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "解析中...",
                            color = iOSLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/* ═════════════════════════ Glass Cards ════════════════════════════════════ */

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth())
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(24.dp))
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = GlassShadow,
                spotColor = GlassShadow
            )
            .padding(20.dp)
    ) {
        content()
    }
}

@Composable
fun GlassCardCompact(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp))
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
    ) {
        content()
    }
}

/* ════════════════════════ Input Card ══════════════════════════════════════ */

@Composable
fun InputCard(videoUrl: String, onUrlChanged: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) iOSBlue else iOSSeparator,
        animationSpec = tween(250),
        label = "border"
    )
    val elevation by animateDpAsState(
        if (focused) 6.dp else 2.dp,
        animationSpec = tween(250),
        label = "elevation"
    )
    val bgColor by animateColorAsState(
        if (focused) iOSCardBg else Color(0xFFF8F8FC),
        animationSpec = tween(250),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth())
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(20.dp),
                ambientColor = GlassShadow,
                spotColor = GlassShadow
            )
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (focused)
                            Brush.linearGradient(listOf(iOSBlue, iOSBlueLight))
                        else
                            Brush.linearGradient(listOf(Color(0xFFE8E8ED), Color(0xFFE0E0E8)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔗",
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            BasicTextField(
                value = videoUrl,
                onValueChange = onUrlChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .onFocusChanged { focused = it.isFocused },
                textStyle = TextStyle(
                    color = iOSLabel,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(iOSBlue),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (videoUrl.isEmpty()) {
                            Text(
                                "粘贴视频链接",
                                color = iOSPlaceholder,
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                }
            )

            // Clear button
            if (videoUrl.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iOSTertiary.copy(alpha = 0.18f))
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = { onUrlChanged("") }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 11.sp, color = iOSSecondary)
                }
            }
        }
    }
}

/* ══════════════════════════ Button Row ════════════════════════════════════ */

@Composable
fun ButtonRow(
    isParsing: Boolean,
    hasUrl: Boolean,
    onPaste: (Context) -> Unit,
    onParse: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Paste button
        PressButton(
            modifier = Modifier.weight(1f),
            onClick = { onPaste(context) },
            background = Color(0xFFF2F2F7),
            shadowColor = Color.Black.copy(alpha = 0.04f),
            contentColor = iOSLabel
        ) {
            Text(
                "粘贴",
                color = iOSLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Parse button
        val enabled = hasUrl && !isParsing
        PressButton(
            modifier = Modifier.weight(1f),
            onClick = { if (enabled) onParse() },
            background = if (enabled) iOSBlue else Color(0xFFB0C4DE),
            shadowColor = if (enabled) iOSBlue.copy(alpha = 0.3f) else Color.Transparent,
            contentColor = Color.White,
            enabled = enabled
        ) {
            Text(
                if (isParsing) "解析中..." else "解析",
                color = Color.White.copy(alpha = if (enabled) 1f else 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.25.sp
            )
        }
    }
}

/* ════════════════════════ Result Section ══════════════════════════════════ */

@Composable
fun ResultSection(
    data: VideoData,
    isDownloading: Boolean,
    downloadPercent: Int,
    isGettingShortLink: Boolean,
    onDownload: () -> Unit,
    onCopyLongLink: () -> Unit,
    onCopyShortLink: () -> Unit,
    onShare: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().widthIn(max = cardMaxWidth())) {
        // Video card
        VideoCard(data)
        Spacer(Modifier.height(16.dp))

        // Action grid
        ActionGrid(
            isDownloading = isDownloading,
            isGettingShortLink = isGettingShortLink,
            onDownload = onDownload,
            onCopyLongLink = onCopyLongLink,
            onCopyShortLink = onCopyShortLink,
            onShare = onShare
        )

        // Progress bar
        AnimatedVisibility(
            visible = isDownloading,
            enter = expandVertically(spring()) + fadeIn(),
            exit = shrinkVertically(spring()) + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                ProgressSection(downloadPercent)
            }
        }
    }
}

@Composable
fun VideoCard(data: VideoData) {
    GlassCard {
        Column {
            // Cover image with overlay
            if (data.coverUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = data.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Bottom gradient overlay for better text readability
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                                )
                            )
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (data.title.isNotBlank()) {
                Text(
                    text = data.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = iOSLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.25).sp
                )
                Spacer(Modifier.height(12.dp))
            }

            data.author?.let { author ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(iOSBlue, iOSPurple))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = author.name.first().toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "@${author.name}",
                        color = iOSSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/* ═════════════════════════ Action Grid ════════════════════════════════════ */

@Composable
fun ActionGrid(
    isDownloading: Boolean,
    isGettingShortLink: Boolean,
    onDownload: () -> Unit,
    onCopyLongLink: () -> Unit,
    onCopyShortLink: () -> Unit,
    onShare: () -> Unit
) {
    val actions = listOf(
        ActionItem(
            label = if (isDownloading) "下载中" else "下载视频",
            iconRes = R.drawable.ic_action_1,
            gradient = GreenGradient,
            shadowColor = iOSGreen.copy(alpha = 0.12f),
            disabled = isDownloading,
            onClick = onDownload
        ),
        ActionItem(
            label = "复制链接",
            iconRes = R.drawable.ic_action_2,
            gradient = AccentGradient,
            shadowColor = iOSBlue.copy(alpha = 0.12f),
            disabled = false,
            onClick = onCopyLongLink
        ),
        ActionItem(
            label = if (isGettingShortLink) "获取中" else "短链接",
            iconRes = R.drawable.ic_action_3,
            gradient = OrangeGradient,
            shadowColor = iOSOrange.copy(alpha = 0.12f),
            disabled = isGettingShortLink,
            onClick = onCopyShortLink
        ),
        ActionItem(
            label = "分享",
            iconRes = R.drawable.ic_action_4,
            gradient = PurpleGradient,
            shadowColor = iOSPurple.copy(alpha = 0.12f),
            disabled = false,
            onClick = onShare
        )
    )

    // Responsive: 2 columns on phones, 4 columns on tablets
    val config = LocalConfiguration.current
    val isWide = config.screenWidthDp >= 600

    if (isWide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.forEach { action ->
                Box(modifier = Modifier.weight(1f)) {
                    ActionCell(action)
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.chunked(2).forEach { pair ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { action ->
                        ActionCell(action)
                    }
                }
            }
        }
    }
}

private data class ActionItem(
    val label: String,
    val iconRes: Int,
    val gradient: Brush,
    val shadowColor: Color,
    val disabled: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun ActionCell(item: ActionItem) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp))
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = item.shadowColor,
                spotColor = item.shadowColor
            )
            .then(
                if (!item.disabled)
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            pressed = true
                            item.onClick()
                        }
                    )
                else Modifier
            )
            .then(if (item.disabled) Modifier.graphicsLayer { alpha = 0.45f } else Modifier)
            .padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(item.gradient),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = item.label,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                item.label,
                fontSize = 12.sp,
                color = iOSSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp
            )
        }
    }

    // Reset pressed state
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(150)
            pressed = false
        }
    }
}

/* ═════════════════════════ Progress ═══════════════════════════════════════ */

@Composable
fun ProgressSection(percent: Int) {
    GlassCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "正在下载视频...",
                    color = iOSLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    "$percent%",
                    color = iOSBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE5E5EA))
            ) {
                val animatedPercent by animateFloatAsState(
                    percent / 100f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "prog"
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedPercent)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(iOSBlue, iOSBlueLight)
                            )
                        )
                )
            }
        }
    }
}

/* ══════════════════════════ Empty State ═══════════════════════════════════ */

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth())
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Decorative ring + icon
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer decorative ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFF0F0F8).copy(alpha = 0.6f),
                                Color(0xFFE8E8F5).copy(alpha = 0.6f)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(iOSBlue.copy(alpha = 0.15f), iOSPurple.copy(alpha = 0.15f))
                        ),
                        CircleShape
                    )
            )
            // Inner icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(iOSBlue.copy(alpha = 0.12f), iOSPurple.copy(alpha = 0.12f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🎬", fontSize = 32.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "粘贴短视频链接",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = iOSLabel,
            letterSpacing = (-0.3).sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "一键解析无水印视频",
            fontSize = 14.sp,
            color = iOSSecondary,
            letterSpacing = (-0.1).sp
        )
    }
}

/* ═════════════════════════ PressButton ════════════════════════════════════ */

@Composable
fun PressButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    background: Color,
    shadowColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = modifier
            .height(54.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .then(
                if (enabled && onClick != {})
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            pressed = true
                            onClick()
                        }
                    )
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(150)
            pressed = false
        }
    }
}
