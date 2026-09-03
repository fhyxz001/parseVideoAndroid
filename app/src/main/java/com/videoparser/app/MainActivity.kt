package com.videoparser.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

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
            VideoParserScreen(viewModel)
        }
    }
}

/* ═══════════════════════ iOS Design System - Colors ═══════════════════════ */

// Primary - iOS System Blue
private val iOSBlue       = Color(0xFF007AFF)
private val iOSBlueLight  = Color(0xFF5AC8FA)

// Semantic
private val iOSGreen      = Color(0xFF34C759)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = cardMaxWidth()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "视频解析助手",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSLabel,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )
                // 服务器设置入口
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = vm::openServerSettings
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.setting),
                        contentDescription = "设置",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
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
                onPasteAndParse = {
                    val ctx = it
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
                    vm.onPasted(text)
                    if (vm.videoUrl.isNotBlank()) {
                        vm.parse()
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // Result or empty state
            val data = vm.parseData
            if (data != null) {
                ResultSection(
                    data = data,
                    isDownloading = vm.isDownloading,
                    downloadPercent = vm.downloadPercent,
                    downloadedBytes = vm.downloadedBytes,
                    downloadTotalBytes = vm.downloadTotalBytes,
                    videoFileSize = vm.videoFileSize,
                    showCoverSaveDialog = vm.showCoverSaveDialog,
                    onCoverClicked = vm::onCoverClicked,
                    onTitleClicked = vm::copyTitle,
                    onAuthorClicked = vm::copyAuthorName,
                    onConfirmSaveCover = vm::confirmSaveCover,
                    onDismissCoverDialog = vm::dismissCoverSaveDialog,
                    onDownload = vm::download
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

        // 服务器设置弹窗
        if (vm.showServerSettings) {
            ServerSettingsDialog(
                serverDraft = vm.serverDraft,
                onUrlChange = vm::onServerDraftChanged,
                onSave = vm::saveServerSettings,
                onReset = vm::resetServerSettings,
                onDismiss = vm::dismissServerSettings
            )
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
    onPasteAndParse: (Context) -> Unit
) {
    val context = LocalContext.current
    val enabled = !isParsing
    PressButton(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth()),
        onClick = { if (enabled) onPasteAndParse(context) },
        background = if (enabled) iOSBlue else Color(0xFFB0C4DE),
        shadowColor = if (enabled) iOSBlue.copy(alpha = 0.3f) else Color.Transparent,
        contentColor = Color.White,
        enabled = enabled
    ) {
        Text(
            if (isParsing) "解析中..." else "粘贴并解析",
            color = Color.White.copy(alpha = if (enabled) 1f else 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.25.sp
        )
    }
}

/* ════════════════════════ Result Section ══════════════════════════════════ */

@Composable
fun ResultSection(
    data: VideoData,
    isDownloading: Boolean,
    downloadPercent: Int,
    downloadedBytes: Long,
    downloadTotalBytes: Long,
    videoFileSize: Long,
    showCoverSaveDialog: Boolean,
    onCoverClicked: () -> Unit,
    onTitleClicked: () -> Unit,
    onAuthorClicked: () -> Unit,
    onConfirmSaveCover: () -> Unit,
    onDismissCoverDialog: () -> Unit,
    onDownload: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().widthIn(max = cardMaxWidth())) {
        // Video card
        VideoCard(
            data = data,
            onCoverClicked = onCoverClicked,
            onTitleClicked = onTitleClicked,
            onAuthorClicked = onAuthorClicked
        )
        Spacer(Modifier.height(16.dp))

        // 视频文件大小
        if (videoFileSize > 0) {
            Text(
                text = "视频大小：${formatFileSize(videoFileSize)}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = iOSSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
        }

        // Download button
        DownloadButton(
            isDownloading = isDownloading,
            onDownload = onDownload
        )

        // Progress bar
        AnimatedVisibility(
            visible = isDownloading,
            enter = expandVertically(spring()) + fadeIn(),
            exit = shrinkVertically(spring()) + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                ProgressSection(
                    percent = downloadPercent,
                    downloadedBytes = downloadedBytes,
                    totalBytes = downloadTotalBytes,
                    knownFileSize = videoFileSize
                )
            }
        }
    }

    // Cover save confirmation dialog
    if (showCoverSaveDialog) {
        AlertDialog(
            onDismissRequest = onDismissCoverDialog,
            title = { Text("保存封面图片") },
            text = { Text("是否将视频封面保存到相册？") },
            confirmButton = {
                TextButton(onClick = onConfirmSaveCover) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCoverDialog) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun VideoCard(
    data: VideoData,
    onCoverClicked: () -> Unit = {},
    onTitleClicked: () -> Unit = {},
    onAuthorClicked: () -> Unit = {}
) {
    GlassCard {
        Column {
            // Cover image with overlay
            if (data.coverUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onCoverClicked
                        )
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
                    letterSpacing = (-0.25).sp,
                    modifier = Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onTitleClicked
                    )
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
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onAuthorClicked
                        )
                    )
                }
            }
        }
    }
}

/* ═════════════════════════ Download Button ═════════════════════════════════ */

@Composable
fun DownloadButton(
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    val enabled = !isDownloading
    PressButton(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth()),
        onClick = { if (enabled) onDownload() },
        background = if (enabled) iOSGreen else Color(0xFFB0C4DE),
        shadowColor = if (enabled) iOSGreen.copy(alpha = 0.3f) else Color.Transparent,
        contentColor = Color.White,
        enabled = enabled
    ) {
        Text(
            if (isDownloading) "下载中..." else "下载视频",
            color = Color.White.copy(alpha = if (enabled) 1f else 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.25.sp
        )
    }
}

/* ═════════════════════════ Progress ═══════════════════════════════════════ */

@Composable
fun ProgressSection(
    percent: Int,
    downloadedBytes: Long,
    totalBytes: Long,
    knownFileSize: Long
) {
    val actualTotal = if (totalBytes > 0) totalBytes else knownFileSize
    val hasTotal = actualTotal > 0
    val displayPercent = if (hasTotal) {
        if (percent >= 0) percent else (downloadedBytes * 100 / actualTotal).toInt().coerceIn(0, 100)
    } else {
        -1
    }

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
                    if (displayPercent >= 0) "$displayPercent%" else "",
                    color = iOSBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            if (downloadedBytes > 0 || hasTotal) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (hasTotal) {
                        "${formatFileSize(downloadedBytes)} / ${formatFileSize(actualTotal)}"
                    } else {
                        "已下载 ${formatFileSize(downloadedBytes)}"
                    },
                    color = iOSSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(14.dp))

            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (displayPercent >= 0) 8.dp else 20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE5E5EA))
            ) {
                if (displayPercent >= 0) {
                    val animatedPercent by animateFloatAsState(
                        displayPercent / 100f,
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
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(16.dp),
                        color = iOSBlue,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "未知大小"
    val value = bytes.toDouble()
    return when {
        value >= 1.0 * 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", value / (1024 * 1024 * 1024))
        value >= 1.0 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", value / (1024 * 1024))
        value >= 1.0 * 1024 -> String.format(Locale.US, "%.1f KB", value / 1024)
        else -> "$bytes B"
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

/* ════════════════════ Server Settings Dialog ═══════════════════════════════ */

@Composable
fun ServerSettingsDialog(
    serverDraft: String,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("解析服务器设置", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    "设置视频解析服务地址，例如：http://192.168.1.100:8888",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = iOSSecondary
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = serverDraft,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://") }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "恢复默认",
                    fontSize = 13.sp,
                    color = iOSBlue,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onReset
                        )
                        .padding(6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
