package com.videoparser.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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

// 媒体画布（预览/时间轴）
private val MediaSurface  = Color(0xFF16161C)

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
                .padding(horizontal = pad, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            Spacer(Modifier.height(14.dp))

            // Input card
            InputCard(
                videoUrl = vm.videoUrl,
                onUrlChanged = vm::onUrlChanged
            )

            Spacer(Modifier.height(12.dp))

            // Button row
            ButtonRow(
                isParsing = vm.isParsing,
                hasInput = vm.videoUrl.isNotBlank(),
                onPasteAndParse = vm::pasteAndParse
            )

            Spacer(Modifier.height(16.dp))

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
                    clipDurationMs = vm.clipDurationMs,
                    showCoverSaveDialog = vm.showCoverSaveDialog,
                    onCoverClicked = vm::onCoverClicked,
                    onTitleClicked = vm::copyTitle,
                    onAuthorClicked = vm::copyAuthorName,
                    onConfirmSaveCover = vm::confirmSaveCover,
                    onDismissCoverDialog = vm::dismissCoverSaveDialog,
                    onDownload = vm::download,
                    onClipClicked = vm::openClipPage
                )
            } else {
                EmptyState()
            }

            Spacer(Modifier.height(20.dp))
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

        // 剪辑页（全屏覆盖，从右侧滑入，避免误触关闭）
        AnimatedVisibility(
            visible = vm.showClipPage,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            ClipPage(vm)
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
    hasInput: Boolean,
    onPasteAndParse: () -> Unit
) {
    val enabled = !isParsing
    PressButton(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth()),
        onClick = { if (enabled) onPasteAndParse() },
        background = if (enabled) iOSBlue else Color(0xFFB0C4DE),
        shadowColor = if (enabled) iOSBlue.copy(alpha = 0.3f) else Color.Transparent,
        contentColor = Color.White,
        enabled = enabled
    ) {
        Text(
            when {
                isParsing -> "解析中..."
                hasInput -> "开始解析"
                else -> "粘贴并解析"
            },
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
    clipDurationMs: Long,
    showCoverSaveDialog: Boolean,
    onCoverClicked: () -> Unit,
    onTitleClicked: () -> Unit,
    onAuthorClicked: () -> Unit,
    onConfirmSaveCover: () -> Unit,
    onDismissCoverDialog: () -> Unit,
    onDownload: () -> Unit,
    onClipClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().widthIn(max = cardMaxWidth())) {
        // Video card
        VideoCard(
            data = data,
            onCoverClicked = onCoverClicked,
            onTitleClicked = onTitleClicked,
            onAuthorClicked = onAuthorClicked
        )
        Spacer(Modifier.height(10.dp))

        // 视频文件大小 / 时长
        val metaParts = buildList {
            if (videoFileSize > 0) add("大小 ${formatFileSize(videoFileSize)}")
            if (clipDurationMs > 0) add("时长 ${formatClipTime(clipDurationMs)}")
        }
        if (metaParts.isNotEmpty()) {
            Text(
                text = metaParts.joinToString("  ·  "),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = iOSSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(10.dp))
        }

        // 下载 + 剪辑（并排双按钮）
        ActionButtonsRow(
            isDownloading = isDownloading,
            onDownload = onDownload,
            onClipClicked = onClipClicked
        )

        // Progress bar
        AnimatedVisibility(
            visible = isDownloading,
            enter = expandVertically(spring()) + fadeIn(),
            exit = shrinkVertically(spring()) + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
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
                        .height(150.dp)
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
                Spacer(Modifier.height(10.dp))
            }

            if (data.title.isNotBlank()) {
                Text(
                    text = data.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = iOSLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.25).sp,
                    modifier = Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onTitleClicked
                    )
                )
                Spacer(Modifier.height(8.dp))
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

/* ══════════════════ Download & Clip Buttons ═══════════════════════════════ */

/** 下载 + 剪辑并排双按钮，节省纵向空间 */
@Composable
fun ActionButtonsRow(
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onClipClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PressButton(
            modifier = Modifier.weight(1f),
            height = 50.dp,
            onClick = { if (!isDownloading) onDownload() },
            background = if (!isDownloading) iOSGreen else Color(0xFFB0C4DE),
            shadowColor = if (!isDownloading) iOSGreen.copy(alpha = 0.3f) else Color.Transparent,
            contentColor = Color.White
        ) {
            Text(
                if (isDownloading) "下载中..." else "下载视频",
                color = Color.White.copy(alpha = if (!isDownloading) 1f else 0.7f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.25.sp
            )
        }
        PressButton(
            modifier = Modifier.weight(1f),
            height = 50.dp,
            onClick = onClipClicked,
            background = Color(0xFFE9F2FF),
            shadowColor = Color.Transparent,
            contentColor = iOSBlue
        ) {
            Text(
                "剪辑下载",
                color = iOSBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.25.sp
            )
        }
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

            Spacer(Modifier.height(8.dp))

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
    height: Dp = 54.dp,
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
            .height(height)
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

/* ═══════════════════════ Clip Download UI ═════════════════════════════════ */

/** 剪辑页：全屏页面（返回键或左上角返回，避免误触关闭） */
@Composable
fun ClipPage(vm: MainViewModel) {
    BackHandler(enabled = vm.showClipPage) { vm.dismissClipPage() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = cardMaxWidth())
                .align(Alignment.TopCenter)
        ) {
            // ── 页头：返回 + 标题（固定） ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iOSCardBg)
                        .border(1.dp, iOSSeparator, CircleShape)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = vm::dismissClipPage
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 18.sp, color = iOSLabel, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "快速截取",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSLabel,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "选好起点与时长，一键剪出片段",
                        fontSize = 12.sp,
                        color = iOSSecondary
                    )
                }
            }

            // ── 内容（可滚动） ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
            ) {
                when {
                    vm.clipProbing -> ClipProbingView()
                    vm.clipDurationMs <= 0 -> ClipUnavailableView()
                    else -> ClipEditor(vm)
                }
            }

            // ── 底部固定操作栏（安全区 + 软键盘适配） ──
            if (!vm.clipProbing && vm.clipDurationMs > 0) {
                ClipBottomBar(vm)
            }
        }

        // 帧图片保存确认弹窗
        if (vm.showFrameSaveDialog) {
            FrameSaveDialog(vm)
        }
    }
}

@Composable
private fun FrameSaveDialog(vm: MainViewModel) {
    val side = if (vm.frameSaveIsStart) "首帧" else "尾帧"
    AlertDialog(
        onDismissRequest = vm::dismissFrameSaveDialog,
        title = { Text("保存${side}图片", fontWeight = FontWeight.SemiBold) },
        text = { Text("是否将该画面保存到本地相册？") },
        confirmButton = {
            TextButton(onClick = vm::confirmSaveFrame) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = vm::dismissFrameSaveDialog) { Text("取消") }
        }
    )
}

@Composable
private fun ClipProbingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(iOSCardBg)
            .border(1.dp, iOSSeparator, RoundedCornerShape(20.dp))
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = iOSBlue,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("正在获取视频信息", color = iOSLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("马上就好，请稍候", color = iOSSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ClipUnavailableView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(iOSCardBg)
            .border(1.dp, iOSSeparator, RoundedCornerShape(20.dp))
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("😢", fontSize = 30.sp)
        Spacer(Modifier.height(12.dp))
        Text("该视频暂不支持剪辑", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = iOSLabel)
        Spacer(Modifier.height(6.dp))
        Text(
            "无法获取视频时长，可尝试直接下载完整视频",
            fontSize = 12.sp,
            color = iOSSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ClipEditor(vm: MainViewModel) {
    val duration = vm.clipDurationMs
    val startMs = vm.clipStartMs
    val endMs = vm.clipEndMs

    // ── 预览：首尾帧静态画面（拖动起止点时实时刷新） ──
    ClipFramePreviews(vm)
    Spacer(Modifier.height(14.dp))

    // ── 快速截取设置：开始时间 + 快捷时长 + 片段信息 ──
    ClipQuickSettings(vm)
    Spacer(Modifier.height(16.dp))

    // ── 片段选择时间轴：拖动开始/结束手柄调整范围 ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("片段时间轴", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSSecondary)
        Text("拖动两端手柄调整范围", fontSize = 11.sp, color = iOSSecondary.copy(alpha = 0.75f))
    }
    Spacer(Modifier.height(8.dp))
    ClipTimelineStrip(
        durationMs = duration,
        startMs = startMs,
        endMs = endMs,
        thumbs = vm.clipThumbs,
        enabled = !vm.isClipping,
        allowManualZoom = true,
        onStartHandleDrag = { t -> vm.setClipStartKeepingDuration(t) },
        onEndHandleDrag = { t -> vm.setClipEndManual(t) }
    )
}

/**
 * 快速截取设置区（核心操作区）：
 * 开始时间（可点击精确输入）+ 快捷截取时长（5/10/15/30/自定义）+ 片段信息。
 */
@Composable
private fun ClipQuickSettings(vm: MainViewModel) {
    var showStartInput by remember { mutableStateOf(false) }
    var showCustomInput by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(iOSCardBg)
            .border(1.dp, iOSSeparator, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // 开始时间：点击进入精确输入
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(iOSBgBase)
                .clickable(interactionSource = null, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showStartInput = true
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("开始时间", fontSize = 11.sp, color = iOSSecondary)
                Spacer(Modifier.height(3.dp))
                Text(
                    formatClipSeconds(vm.clipStartMs),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSLabel,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.5).sp
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(iOSBlue.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("精确输入", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = iOSBlue)
                Spacer(Modifier.width(2.dp))
                Text("›", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("截取时长", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSSecondary)
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MainViewModel.CLIP_PRESET_SECONDS.forEach { sec ->
                DurationChip(
                    label = "${sec}秒",
                    selected = !vm.clipCustomDuration && vm.clipPresetSec == sec
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.applyClipPreset(sec)
                }
            }
            DurationChip(label = "自定义", selected = vm.clipCustomDuration) {
                showCustomInput = true
            }
        }
        Spacer(Modifier.height(16.dp))

        Box(Modifier.fillMaxWidth().height(1.dp).background(iOSSeparator))
        Spacer(Modifier.height(12.dp))

        // 片段信息：截取范围 + 实际时长
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("片段范围", fontSize = 11.sp, color = iOSSecondary)
                Spacer(Modifier.height(3.dp))
                Text(
                    "${formatClipSeconds(vm.clipStartMs)} — ${formatClipSeconds(vm.clipEndMs)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSLabel,
                    fontFamily = FontFamily.Monospace
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(iOSBlue.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "共 ${formatSecondsShort(vm.clipSelectedSeconds)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSBlue
                )
            }
        }
    }

    if (showStartInput) {
        ClipInputDialog(
            title = "输入开始时间",
            label = "秒 / 分:秒 / 时:分:秒",
            hint = "例如 32.5 或 1:05",
            initialText = secondsInputText(vm.clipStartMs),
            keyboardType = KeyboardType.Ascii,
            parse = { raw -> parseClipSeconds(raw) },
            onApply = { sec ->
                val ms = (sec * 1000).roundToLong()
                vm.setClipStartKeepingDuration(ms)
            },
            onDismiss = { showStartInput = false }
        )
    }
    if (showCustomInput) {
        ClipInputDialog(
            title = "自定义截取时长",
            label = "截取秒数",
            hint = "例如 12.5",
            initialText = String.format(Locale.US, "%.1f", vm.clipSelectedSeconds),
            keyboardType = KeyboardType.Decimal,
            parse = { raw -> parseClipSeconds(raw)?.takeIf { it > 0 } },
            onApply = { sec -> vm.applyClipCustomDuration(sec) },
            onDismiss = { showCustomInput = false }
        )
    }
}

/** 快捷时长药丸：选中态主色填充 + 柔和投影，未选中为浅底描边 */
@Composable
private fun DurationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) iOSBlue else iOSBgBase)
            .then(
                if (selected) {
                    Modifier.shadow(
                        6.dp,
                        RoundedCornerShape(50),
                        ambientColor = iOSBlue.copy(alpha = 0.45f),
                        spotColor = iOSBlue.copy(alpha = 0.45f)
                    )
                } else {
                    Modifier.border(1.dp, iOSSeparator, RoundedCornerShape(50))
                }
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else iOSLabel
        )
    }
}

/** 时间/时长输入弹窗：确认时才做合法性校验，输入过程中不重置内容 */
@Composable
private fun ClipInputDialog(
    title: String,
    label: String,
    hint: String,
    initialText: String,
    keyboardType: KeyboardType,
    parse: (String) -> Double?,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var error by remember { mutableStateOf<String?>(null) }

    fun confirm() {
        val value = parse(text)
        if (value == null) {
            error = "请输入有效的时间"
        } else {
            onApply(value)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    singleLine = true,
                    label = { Text(label) },
                    placeholder = { Text(hint) },
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = Color(0xFFFF3B30))
                }
            }
        },
        confirmButton = { TextButton(onClick = { confirm() }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/**
 * 底部固定操作栏：截取摘要 + 唯一主按钮，处理中显示进度，成功后展示完成状态。
 */
@Composable
private fun ClipBottomBar(vm: MainViewModel) {
    val context = LocalContext.current
    val resultName = vm.clipResultFileName

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8FC))
            .drawBehind {
                drawLine(
                    color = iOSSeparator,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 12.dp)
    ) {
        when {
            resultName != null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(iOSGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "剪辑完成，已保存到相册",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iOSLabel
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            resultName,
                            fontSize = 11.sp,
                            color = iOSSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(iOSBgBase)
                            .border(1.dp, iOSSeparator, RoundedCornerShape(14.dp))
                            .clickable(interactionSource = null, indication = null) {
                                openClipResult(context, resultName)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "打开相册",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iOSLabel
                        )
                    }
                    PressButton(
                        modifier = Modifier.weight(1f),
                        onClick = vm::dismissClipResult,
                        background = iOSBlue,
                        shadowColor = iOSBlue.copy(alpha = 0.3f),
                        contentColor = Color.White,
                        height = 48.dp
                    ) {
                        Text(
                            "完成",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            vm.isClipping -> ClipProgressArea(
                phaseText = vm.clipPhaseText,
                percent = vm.clipPercent,
                onCancel = vm::cancelClip
            )

            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("已选择", fontSize = 12.sp, color = iOSSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${formatClipTime(vm.clipStartMs)}–${formatClipTime(vm.clipEndMs)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSLabel,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(iOSGreen.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            formatSecondsShort(vm.clipSelectedSeconds),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iOSGreen
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val estimatedBytes = if (vm.videoFileSize > 0 && vm.clipDurationMs > 0) {
                    vm.videoFileSize * (vm.clipEndMs - vm.clipStartMs).coerceAtLeast(0L) / vm.clipDurationMs
                } else 0L
                PressButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = cardMaxWidth()),
                    onClick = vm::startClipDownload,
                    background = iOSGreen,
                    shadowColor = iOSGreen.copy(alpha = 0.3f),
                    contentColor = Color.White
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✂", fontSize = 14.sp, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (estimatedBytes > 0) "剪辑并下载（约 ${formatFileSize(estimatedBytes)}）"
                            else "剪辑并下载",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.25.sp
                        )
                    }
                }
            }
        }
    }
}

/** 打开刚剪辑好的视频：优先定位到相册中的该文件，失败则打开系统视频列表 */
private fun openClipResult(context: Context, fileName: String) {
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val uri = runCatching {
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.DISPLAY_NAME}=?",
            arrayOf(fileName),
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
        }
    }.getOrNull()
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri ?: collection, "video/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

/** 时间轴时间显示：mm:ss.d（带一位小数，便于秒级微调） */
private fun formatClipSeconds(ms: Long): String {
    val totalTenths = (ms.coerceAtLeast(0L) + 50) / 100
    val totalSec = totalTenths / 10
    val tenth = totalTenths % 10
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d.%d", h, m, s, tenth)
    } else {
        String.format(Locale.US, "%02d:%02d.%d", m, s, tenth)
    }
}

/** 时长展示：整秒省略小数（10 秒 / 7.5 秒） */
private fun formatSecondsShort(seconds: Double): String {
    val rounded = (seconds * 10).roundToLong() / 10.0
    return if (kotlin.math.abs(rounded - rounded.toLong()) < 0.05) {
        "${rounded.toLong()} 秒"
    } else {
        String.format(Locale.US, "%.1f 秒", rounded)
    }
}

/** 时间轴最大缩放倍率：随视频时长自适应，保证放大到顶时视口至少跨约 15 秒，长视频也能精细选段 */
private fun maxZoomFor(durationMs: Long): Float = (durationMs / 15_000f).coerceIn(20f, 120f)

/**
 * 双滑块时间轴：缩略图条 + 选区高亮 + 可拖拽把手。
 * 播放器模式：点击时间轴跳转播放位置（onTimelineTap），拖动空白区刷视频预览（onTimelineScrub）。
 * 拖动开始手柄保持当前时长、结束时间联动（onStartHandleDrag）；拖动结束手柄重算时长（onEndHandleDrag）。
 * 降级模式：保留双指缩放（以手势中心为锚点），点击就近移动对应手柄。
 * 选区在屏幕上过窄时（拖动把手触发）自动放大，避免长视频下小选区难以微调。
 */
@Composable
private fun ClipTimelineStrip(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    thumbs: List<ImageBitmap?>,
    enabled: Boolean,
    playheadMs: Long? = null,
    allowManualZoom: Boolean = false,
    onStartHandleDrag: (Long) -> Unit,
    onEndHandleDrag: (Long) -> Unit,
    onTimelineTap: ((Long) -> Unit)? = null,
    onTimelineScrub: ((Long) -> Unit)? = null,
    onSeekPreview: ((Long) -> Unit)? = null,
    onDragStateChange: ((Boolean) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    // pointerInput 不随重组重启，用 rememberUpdatedState 保证手势回调读到最新值
    val currentStartMs by rememberUpdatedState(startMs)
    val currentEndMs by rememberUpdatedState(endMs)
    val currentOnStartHandleDrag by rememberUpdatedState(onStartHandleDrag)
    val currentOnEndHandleDrag by rememberUpdatedState(onEndHandleDrag)
    val currentOnTimelineTap by rememberUpdatedState(onTimelineTap)
    val currentOnTimelineScrub by rememberUpdatedState(onTimelineScrub)
    val currentOnSeekPreview by rememberUpdatedState(onSeekPreview)
    val currentOnDragStateChange by rememberUpdatedState(onDragStateChange)
    var activeHandle by remember { mutableStateOf<Int?>(null) }
    // 缩放倍率与视口偏移（内容像素）；换视频后重置
    var zoom by remember(durationMs) { mutableFloatStateOf(1f) }
    var scrollX by remember(durationMs) { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        val stripWpx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val handleW = 14.dp
        val handleWpx = with(density) { handleW.toPx() }
        // 手柄视觉宽度保持纤细，但触摸区域加宽到 40dp，避免竖屏下难以精确拖动
        val handleTouchW = 40.dp
        val handleTouchWpx = with(density) { handleTouchW.toPx() }
        val edgeMarginPx = with(density) { 20.dp.toPx() }
        val maxZoom = maxZoomFor(durationMs)

        // 选区在屏幕上不足 40% 宽时自动放大到约 55%，锚点（anchorScreenX）保持不动，
        // 解决长视频下小选区把手重叠、难以精确拖动的问题
        fun autoZoomForNarrowSelection(selStartMs: Long, selEndMs: Long, anchorMs: Long, anchorScreenX: Float) {
            if (durationMs <= 0L) return
            val selFrac = (selEndMs - selStartMs).toFloat() / durationMs
            if (selFrac <= 0f || selFrac * zoom >= 0.4f) return
            val target = (0.55f / selFrac).coerceIn(zoom, maxZoom)
            if (target <= zoom * 1.001f) return
            zoom = target
            scrollX = (contentOfMs(anchorMs, stripWpx, target, durationMs) - anchorScreenX)
                .coerceIn(0f, (stripWpx * target - stripWpx).coerceAtLeast(0f))
        }

        val startContentPx = contentOfMs(startMs, stripWpx, zoom, durationMs)
        val endContentPx = contentOfMs(endMs, stripWpx, zoom, durationMs)
        val selLeftScreen = startContentPx - scrollX
        val selWidthScreen = (endContentPx - startContentPx).coerceAtLeast(handleWpx)
        val selRightScreen = selLeftScreen + selWidthScreen

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MediaSurface)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .then(
                    if (enabled) Modifier
                        // 播放器模式：在时间轴上拖动 = 刷视频预览（把手区域由把手优先消费）
                        .then(
                            if (currentOnTimelineScrub != null) Modifier.pointerInput(durationMs) {
                                detectHorizontalDragGestures(
                                    onDragStart = { _ -> currentOnDragStateChange?.invoke(true) },
                                    onDragEnd = { currentOnDragStateChange?.invoke(false) },
                                    onDragCancel = { currentOnDragStateChange?.invoke(false) }
                                ) { change, _ ->
                                    if (!change.isConsumed) {
                                        val t = msOfContent(change.position.x + scrollX, stripWpx, zoom, durationMs)
                                        currentOnTimelineScrub?.invoke(t)
                                    }
                                }
                            } else Modifier
                        )
                        // 双指缩放/平移（仅降级模式保留；播放器模式靠播放定位）
                        .then(
                            if (allowManualZoom) Modifier.pointerInput(durationMs) {
                                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                                    if (gestureZoom != 1f) {
                                        val newZoom = (zoom * gestureZoom).coerceIn(1f, maxZoom)
                                        val newContentW = stripWpx * newZoom
                                        val tAtCentroid = (centroid.x + scrollX) / (stripWpx * zoom) * durationMs
                                        scrollX = (tAtCentroid / durationMs * newContentW - centroid.x)
                                            .coerceIn(0f, (newContentW - stripWpx).coerceAtLeast(0f))
                                        zoom = newZoom
                                    } else if (zoom > 1.01f && pan.x != 0f) {
                                        scrollX = (scrollX - pan.x)
                                            .coerceIn(0f, (stripWpx * zoom - stripWpx).coerceAtLeast(0f))
                                    }
                                }
                            } else Modifier
                        )
                        .pointerInput(durationMs) {
                            detectTapGestures(
                                onDoubleTap = if (allowManualZoom) {
                                    { zoom = 1f; scrollX = 0f }
                                } else null,
                                onTap = { offset ->
                                    val t = msOfContent(offset.x + scrollX, stripWpx, zoom, durationMs)
                                    val tapCb = currentOnTimelineTap
                                    if (tapCb != null) {
                                        // 播放器模式：点击时间轴跳转播放位置
                                        tapCb(t)
                                    } else {
                                        // 降级模式：点击就近移动对应手柄
                                        val distStart = kotlin.math.abs(t - currentStartMs)
                                        val distEnd = kotlin.math.abs(currentEndMs - t)
                                        if (distStart <= distEnd) {
                                            currentOnStartHandleDrag(t)
                                            autoZoomForNarrowSelection(t, currentEndMs, t, offset.x)
                                        } else {
                                            currentOnEndHandleDrag(t)
                                            autoZoomForNarrowSelection(currentStartMs, t, t, offset.x)
                                        }
                                    }
                                }
                            )
                        }
                    else Modifier
                )
        ) {
            // 缩略图
            Row(modifier = Modifier.fillMaxSize()) {
                if (thumbs.isEmpty()) {
                    PulsingPlaceholder(modifier = Modifier.fillMaxSize())
                } else {
                    thumbs.forEach { thumb ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(Color(0xFF2C2C2E)))
                            }
                        }
                    }
                }
            }

            // 选区内轻微提亮
            Box(
                Modifier
                    .fillMaxHeight()
                    .absoluteOffset { IntOffset(selLeftScreen.roundToInt(), 0) }
                    .width(with(density) { selWidthScreen.coerceAtLeast(0f).toDp() })
                    .background(iOSBlue.copy(alpha = 0.18f))
            )

            // 选区外的压暗层
            if (selLeftScreen > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(with(density) { selLeftScreen.coerceAtMost(stripWpx).toDp() })
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
            if (selRightScreen < stripWpx) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .width(with(density) { (stripWpx - selRightScreen).coerceAtLeast(0f).toDp() })
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            // 选区边框
            Box(
                Modifier
                    .fillMaxHeight()
                    .absoluteOffset { IntOffset(selLeftScreen.roundToInt(), 0) }
                    .width(with(density) { selWidthScreen.toDp() })
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
            )

            // 播放头指示线
            if (playheadMs != null) {
                val phScreenX = contentOfMs(playheadMs, stripWpx, zoom, durationMs) - scrollX
                if (phScreenX >= -2f && phScreenX <= stripWpx + 2f) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .absoluteOffset { IntOffset(phScreenX.roundToInt() - 1, 0) }
                            .width(2.dp)
                            .background(Color(0xFFFFD60A))
                    )
                }
            }

            // 左把手
            ClipHandle(
                modifier = Modifier
                    .fillMaxHeight()
                    .absoluteOffset { IntOffset((selLeftScreen - handleTouchWpx / 2).roundToInt(), 0) }
                    .width(handleTouchW),
                visualWidth = handleW,
                enabled = enabled,
                active = activeHandle == 0,
                onDragStart = {
                    activeHandle = 0
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnDragStateChange?.invoke(true)
                },
                onDragEnd = {
                    activeHandle = null
                    currentOnDragStateChange?.invoke(false)
                },
                onDrag = { dragAmount ->
                    val newT = msOfContent(contentOfMs(currentStartMs, stripWpx, zoom, durationMs) + dragAmount, stripWpx, zoom, durationMs)
                    currentOnStartHandleDrag(newT)
                    // 注意 anchorScreenX 需在 zoom 被修改前求值，保证把手停留在手指原位置
                    autoZoomForNarrowSelection(
                        selStartMs = newT,
                        selEndMs = currentEndMs,
                        anchorMs = newT,
                        anchorScreenX = contentOfMs(newT, stripWpx, zoom, durationMs) - scrollX
                    )
                    panViewportTo(
                        contentX = contentOfMs(newT, stripWpx, zoom, durationMs),
                        handleScreenX = contentOfMs(newT, stripWpx, zoom, durationMs) - scrollX,
                        stripWpx = stripWpx,
                        zoom = zoom,
                        edgeMarginPx = edgeMarginPx
                    ) { scrollX = it }
                    currentOnSeekPreview?.invoke(newT)
                }
            )
            // 右把手
            ClipHandle(
                modifier = Modifier
                    .fillMaxHeight()
                    .absoluteOffset { IntOffset((selRightScreen - handleTouchWpx / 2).roundToInt(), 0) }
                    .width(handleTouchW),
                visualWidth = handleW,
                enabled = enabled,
                active = activeHandle == 1,
                onDragStart = {
                    activeHandle = 1
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnDragStateChange?.invoke(true)
                },
                onDragEnd = {
                    activeHandle = null
                    currentOnDragStateChange?.invoke(false)
                },
                onDrag = { dragAmount ->
                    val newT = msOfContent(contentOfMs(currentEndMs, stripWpx, zoom, durationMs) + dragAmount, stripWpx, zoom, durationMs)
                    currentOnEndHandleDrag(newT)
                    autoZoomForNarrowSelection(
                        selStartMs = currentStartMs,
                        selEndMs = newT,
                        anchorMs = newT,
                        anchorScreenX = contentOfMs(newT, stripWpx, zoom, durationMs) - scrollX
                    )
                    panViewportTo(
                        contentX = contentOfMs(newT, stripWpx, zoom, durationMs),
                        handleScreenX = contentOfMs(newT, stripWpx, zoom, durationMs) - scrollX,
                        stripWpx = stripWpx,
                        zoom = zoom,
                        edgeMarginPx = edgeMarginPx
                    ) { scrollX = it }
                    currentOnSeekPreview?.invoke(newT)
                }
            )

            // 缩放指示：显示当前视口跨度的视频时长（仅降级模式的手动缩放）
            if (allowManualZoom && zoom > 1.01f) {
                Text(
                    "可视 " + formatClipTime((durationMs / zoom).roundToLong()),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/** 时间 → 内容像素（zoom 缩放后的时间轴宽度） */
private fun contentOfMs(ms: Long, stripWpx: Float, zoom: Float, durationMs: Long): Float =
    ms * stripWpx * zoom / durationMs.coerceAtLeast(1L)

/** 内容像素 → 时间 */
private fun msOfContent(px: Float, stripWpx: Float, zoom: Float, durationMs: Long): Long =
    (px / (stripWpx * zoom) * durationMs).roundToLong().coerceIn(0L, durationMs)

/** 拖把手到边缘时自动平移视口，保证长视频放大后也能继续拖 */
private inline fun panViewportTo(
    contentX: Float,
    handleScreenX: Float,
    stripWpx: Float,
    zoom: Float,
    edgeMarginPx: Float,
    setScroll: (Float) -> Unit
) {
    val maxScroll = (stripWpx * zoom - stripWpx).coerceAtLeast(0f)
    if (handleScreenX > stripWpx - edgeMarginPx) {
        setScroll((contentX - (stripWpx - edgeMarginPx)).coerceIn(0f, maxScroll))
    } else if (handleScreenX < edgeMarginPx) {
        setScroll((contentX - edgeMarginPx).coerceIn(0f, maxScroll))
    }
}

/** 解析手动输入的时间：支持纯秒数（"75" / "75.5"）与 "mm:ss"、"hh:mm:ss"，返回毫秒 */
private fun parseClipTimeInput(raw: String): Long? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    return if (s.contains(':')) {
        val nums = s.split(':').map { it.trim().toDoubleOrNull() ?: return null }
        val seconds = when (nums.size) {
            2 -> nums[0] * 60 + nums[1]
            3 -> nums[0] * 3600 + nums[1] * 60 + nums[2]
            else -> return null
        }
        if (seconds < 0) null else (seconds * 1000).roundToLong()
    } else {
        s.toDoubleOrNull()?.takeIf { it >= 0 }?.let { (it * 1000).roundToLong() }
    }
}

/** 解析输入为秒（供输入弹窗使用） */
private fun parseClipSeconds(raw: String): Double? =
    parseClipTimeInput(raw)?.let { it / 1000.0 }

/** 输入框展示用：秒（保留 1 位小数） */
private fun secondsInputText(ms: Long): String =
    String.format(Locale.US, "%.1f", ms / 1000.0)

/**
 * 首帧/尾帧双图预览：上方两块画面分别对应剪辑的起点与终点，
 * 时间调整时实时刷新，帮助用户快速确认剪辑区间。
 */
@Composable
private fun ClipFramePreviews(vm: MainViewModel) {
    // 起止时间变化（拖动/步进/点击）时自动请求对应帧
    LaunchedEffect(vm.clipStartMs) { vm.requestClipFrame(isStart = true, timeMs = vm.clipStartMs) }
    LaunchedEffect(vm.clipEndMs) { vm.requestClipFrame(isStart = false, timeMs = vm.clipEndMs) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ClipFrameCard(
            modifier = Modifier.weight(1f),
            label = "开始",
            labelColor = iOSBlue,
            timeMs = vm.clipStartMs,
            frame = vm.clipStartFrame,
            onClick = { vm.onClipFrameClicked(isStart = true) }
        )
        ClipFrameCard(
            modifier = Modifier.weight(1f),
            label = "结束",
            labelColor = iOSGreen,
            timeMs = vm.clipEndMs,
            frame = vm.clipEndFrame,
            onClick = { vm.onClipFrameClicked(isStart = false) }
        )
    }
}

/** 单张帧预览卡片：画面 + 左上角标签 + 底部时间遮罩，点击可保存到相册 */
@Composable
private fun ClipFrameCard(
    modifier: Modifier = Modifier,
    label: String,
    labelColor: Color,
    timeMs: Long,
    frame: ImageBitmap?,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MediaSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (frame != null) {
            Image(
                bitmap = frame,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PulsingPlaceholder(modifier = Modifier.fillMaxSize())
            CircularProgressIndicator(
                color = iOSBlue,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        }

        // 底部渐变遮罩，保证时间文字在任意画面上清晰
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                    )
                )
        )
        Text(
            formatClipTime(timeMs),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 8.dp)
        )
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(labelColor.copy(alpha = 0.92f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** 缩略图未就绪时的呼吸占位动画 */
@Composable
private fun PulsingPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thumbPulse")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "thumbPulseAlpha"
    )
    Box(
        modifier
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF232329), Color(0xFF33333C), Color(0xFF232329))
                )
            )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
                .background(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
    }
}

/** 时间轴把手：白色厚重圆角条 + 中央凹槽，拖拽时轻微放大 */
@Composable
private fun ClipHandle(
    modifier: Modifier,
    visualWidth: Dp,
    enabled: Boolean,
    active: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val scale by animateFloatAsState(
        if (active) 1.18f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "handleScale"
    )
    // 外层 Box 承担加宽的触摸区域；内层白色把手按 visualWidth 绘制，保持视觉纤细
    Box(
        modifier = modifier.then(
            if (enabled) Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { currentOnDragStart() },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount)
                }
            } else Modifier
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(visualWidth)
                .shadow(
                    elevation = if (active) 8.dp else 4.dp,
                    shape = RoundedCornerShape(5.dp),
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // 中央凹槽
            Box(
                Modifier
                    .size(width = 2.dp, height = 20.dp)
                    .background(Color(0xFF1C1C1E).copy(alpha = 0.4f), RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
private fun ClipProgressArea(
    phaseText: String,
    percent: Int,
    onCancel: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                phaseText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = iOSLabel
            )
            Text(
                "$percent%",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSBlue,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFE5E5EA))
        ) {
            val animated by animateFloatAsState(
                percent / 100f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "clipProg"
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(iOSBlue, iOSBlueLight)))
            )
        }
        Spacer(Modifier.height(14.dp))
        // 取消：居中柔和药丸
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFF3B30).copy(alpha = 0.1f))
                .border(
                    1.dp,
                    Color(0xFFFF3B30).copy(alpha = 0.35f),
                    RoundedCornerShape(50)
                )
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onCancel
                )
                .padding(horizontal = 24.dp, vertical = 9.dp)
        ) {
            Text(
                "取消剪辑",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFF3B30)
            )
        }
    }
}

/** mm:ss / h:mm:ss 展示 */
private fun formatClipTime(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L)) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}
