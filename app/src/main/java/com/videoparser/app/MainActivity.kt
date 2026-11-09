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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
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
    }
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
    // ── 快速截取设置：开始时间（时/分/秒）+ 快捷时长 + 片段信息 ──
    ClipQuickSettings(vm)
    Spacer(Modifier.height(14.dp))
    Text(
        "提示：为保证画面可独立解码，起点会自动对齐到最近关键帧，实际剪出的区间可能略有偏差。",
        fontSize = 11.sp,
        lineHeight = 17.sp,
        color = iOSSecondary.copy(alpha = 0.75f),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

/**
 * 快速截取设置区（核心操作区）：
 * 开始时间（时 / 分 / 秒 三个输入框，默认 0）+ 快捷截取时长（20/60/120/自定义）+ 片段信息。
 */
@Composable
private fun ClipQuickSettings(vm: MainViewModel) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("开始时间", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSSecondary)
            Text(
                "视频时长 ${formatClipTime(vm.clipDurationMs)}",
                fontSize = 11.sp,
                color = iOSSecondary.copy(alpha = 0.75f)
            )
        }
        Spacer(Modifier.height(10.dp))

        // 时 / 分 / 秒 三个输入框，默认 0，输入即联动结束时间
        ClipStartTimeFields(
            startMs = vm.clipStartMs,
            onStartChanged = vm::setClipStartKeepingDuration
        )
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
                    "${formatClipTime(vm.clipStartMs)} — ${formatClipTime(vm.clipEndMs)}",
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

    if (showCustomInput) {
        ClipCustomDurationDialog(
            initialSeconds = vm.clipSelectedSeconds,
            onApply = { sec -> vm.applyClipCustomDuration(sec) },
            onDismiss = { showCustomInput = false }
        )
    }
}

/**
 * 开始时间输入：时 / 分 / 秒 三个独立输入框，默认 0。
 * 输入时按当前内容实时联动结束时间；失去焦点后回填实际生效的时间（超出视频范围会自动收敛）。
 */
@Composable
private fun ClipStartTimeFields(
    startMs: Long,
    onStartChanged: (Long) -> Unit
) {
    var hoursText by remember { mutableStateOf("0") }
    var minutesText by remember { mutableStateOf("0") }
    var secondsText by remember { mutableStateOf("0") }
    var hoursFocused by remember { mutableStateOf(false) }
    var minutesFocused by remember { mutableStateOf(false) }
    var secondsFocused by remember { mutableStateOf(false) }
    val anyFocused = hoursFocused || minutesFocused || secondsFocused

    fun enteredMs(): Long {
        val h = hoursText.toLongOrNull() ?: 0L
        val m = minutesText.toLongOrNull() ?: 0L
        val s = secondsText.toLongOrNull() ?: 0L
        return (h * 3600L + m * 60L + s) * 1000L
    }

    fun applyEntered() {
        onStartChanged(enteredMs())
    }

    // 外部时间变化（首次探测/重置/超范围收敛）且当前没有输入焦点时，回填到输入框
    LaunchedEffect(startMs, anyFocused) {
        if (!anyFocused) {
            val totalSec = startMs.coerceAtLeast(0L) / 1000
            hoursText = (totalSec / 3600).toString()
            minutesText = ((totalSec % 3600) / 60).toString()
            secondsText = (totalSec % 60).toString()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ClipTimeField(
            value = hoursText,
            placeholder = "0",
            unit = "时",
            modifier = Modifier.weight(1f),
            onFocusChanged = { hoursFocused = it },
            onValueChange = { raw ->
                hoursText = normalizeTimePart(raw, maxValue = 99, maxDigits = 2)
                applyEntered()
            }
        )
        ClipTimeField(
            value = minutesText,
            placeholder = "0",
            unit = "分",
            modifier = Modifier.weight(1f),
            onFocusChanged = { minutesFocused = it },
            onValueChange = { raw ->
                minutesText = normalizeTimePart(raw, maxValue = 59, maxDigits = 2)
                applyEntered()
            }
        )
        ClipTimeField(
            value = secondsText,
            placeholder = "0",
            unit = "秒",
            modifier = Modifier.weight(1f),
            onFocusChanged = { secondsFocused = it },
            onValueChange = { raw ->
                secondsText = normalizeTimePart(raw, maxValue = 59, maxDigits = 2)
                applyEntered()
            }
        )
    }

    if (anyFocused && enteredMs() != startMs) {
        Spacer(Modifier.height(8.dp))
        Text(
            "超出视频范围，将从 ${formatClipTime(startMs)} 开始截取",
            fontSize = 11.sp,
            color = Color(0xFFFF9500)
        )
    }
}

/** 单个时间输入框：数字键盘 + 居中大号数字 + 单位标签 */
@Composable
private fun ClipTimeField(
    value: String,
    placeholder: String,
    unit: String,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iOSBgBase)
                .border(
                    width = if (focused) 1.5.dp else 1.dp,
                    color = if (focused) iOSBlue else iOSSeparator,
                    shape = RoundedCornerShape(14.dp)
                )
                // 输入框外的留白区域也能点击聚焦，避免必须精准点中数字
                .clickable(interactionSource = null, indication = null) {
                    focusRequester.requestFocus()
                },
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        onFocusChanged(it.isFocused)
                    },
                textStyle = TextStyle(
                    color = iOSLabel,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                cursorBrush = SolidColor(iOSBlue),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color = iOSPlaceholder,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        inner()
                    }
                }
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(unit, fontSize = 11.sp, color = iOSSecondary)
    }
}

/** 时间片段输入规范化：仅保留数字，限制位数与最大值 */
private fun normalizeTimePart(raw: String, maxValue: Int, maxDigits: Int): String {
    val digits = raw.filter { it.isDigit() }.take(maxDigits)
    if (digits.isEmpty()) return ""
    return digits.toInt().coerceAtMost(maxValue).toString()
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

/** 自定义截取时长输入弹窗：确认时才做合法性校验，输入过程中不重置内容 */
@Composable
private fun ClipCustomDurationDialog(
    initialSeconds: Double,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialSeconds)) }
    var error by remember { mutableStateOf<String?>(null) }

    fun confirm() {
        val value = text.trim().toDoubleOrNull()
        if (value == null || value <= 0) {
            error = "请输入有效的秒数"
        } else {
            onApply(value)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义截取时长", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    singleLine = true,
                    label = { Text("截取秒数") },
                    placeholder = { Text("例如 12.5") },
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
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

/** 时长展示：整秒省略小数（10 秒 / 7.5 秒） */
private fun formatSecondsShort(seconds: Double): String {
    val rounded = (seconds * 10).roundToLong() / 10.0
    return if (kotlin.math.abs(rounded - rounded.toLong()) < 0.05) {
        "${rounded.toLong()} 秒"
    } else {
        String.format(Locale.US, "%.1f 秒", rounded)
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
