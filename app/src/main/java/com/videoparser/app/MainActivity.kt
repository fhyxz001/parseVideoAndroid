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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
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
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
import kotlin.math.roundToInt

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
                    clipDurationMs = vm.clipDurationMs,
                    showCoverSaveDialog = vm.showCoverSaveDialog,
                    onCoverClicked = vm::onCoverClicked,
                    onTitleClicked = vm::copyTitle,
                    onAuthorClicked = vm::copyAuthorName,
                    onConfirmSaveCover = vm::confirmSaveCover,
                    onDismissCoverDialog = vm::dismissCoverSaveDialog,
                    onDownload = vm::download,
                    onClipClicked = vm::openClipSheet
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

        // 剪辑底部弹窗
        ClipSheet(vm)
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
        Spacer(Modifier.height(16.dp))

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
            Spacer(Modifier.height(12.dp))
        }

        // Download button
        DownloadButton(
            isDownloading = isDownloading,
            onDownload = onDownload
        )

        Spacer(Modifier.height(10.dp))

        // Clip download button
        ClipDownloadButton(onClipClicked = onClipClicked)

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

/* ═══════════════════════ Clip Download UI ═════════════════════════════════ */

@Composable
fun ClipDownloadButton(onClipClicked: () -> Unit) {
    PressButton(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = cardMaxWidth()),
        onClick = onClipClicked,
        background = Color(0xFFE9F2FF),
        shadowColor = Color.Transparent,
        contentColor = iOSBlue
    ) {
        Text(
            "剪辑下载",
            color = iOSBlue,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.25.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipSheet(vm: MainViewModel) {
    if (!vm.showClipSheet) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = vm::dismissClipSheet,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        ClipSheetContent(vm)
    }
}

@Composable
fun ClipSheetContent(vm: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        // ── Header ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "剪辑下载",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = iOSLabel,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iOSTertiary.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = vm::dismissClipSheet
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 12.sp, color = iOSSecondary)
            }
        }
        Spacer(Modifier.height(18.dp))

        when {
            vm.clipProbing -> ClipProbingView()
            vm.clipDurationMs <= 0 -> ClipUnavailableView()
            else -> ClipEditor(vm)
        }
    }
}

@Composable
private fun ClipProbingView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = iOSBlue,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text("正在获取视频信息...", color = iOSSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun ClipUnavailableView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("😢", fontSize = 30.sp)
        Spacer(Modifier.height(10.dp))
        Text("该视频暂不支持剪辑", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = iOSLabel)
        Spacer(Modifier.height(6.dp))
        Text(
            "无法获取视频时长，可尝试直接下载完整视频",
            fontSize = 13.sp,
            color = iOSSecondary
        )
    }
}

@Composable
private fun ClipEditor(vm: MainViewModel) {
    val duration = vm.clipDurationMs
    val startMs = vm.clipStartMs
    val endMs = vm.clipEndMs
    val selected = (endMs - startMs).coerceAtLeast(0L)
    val snapped = vm.snappedClipWindow()
    val estimatedBytes = if (vm.videoFileSize > 0 && duration > 0) {
        vm.videoFileSize * selected / duration
    } else 0L

    // ── 时间轴缩略图 ──
    ClipTimelineStrip(
        durationMs = duration,
        startMs = startMs,
        endMs = endMs,
        thumbs = vm.clipThumbs,
        enabled = !vm.isClipping,
        onRangeChanged = { s, e -> vm.setClipRange(s, e) }
    )
    Spacer(Modifier.height(6.dp))

    // ── 时间读数 ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            formatClipTime(startMs),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSLabel,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "已选 ${formatClipTime(selected)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSBlue
        )
        Text(
            formatClipTime(endMs),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSLabel,
            fontFamily = FontFamily.Monospace
        )
    }
    Spacer(Modifier.height(12.dp))

    // ── 微调 ──
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TimeStepperCard(
            modifier = Modifier.weight(1f),
            label = "开始",
            timeText = formatClipTime(startMs),
            onMinus = { vm.nudgeClipStart(-1000L) },
            onPlus = { vm.nudgeClipStart(1000L) },
            enabled = !vm.isClipping
        )
        TimeStepperCard(
            modifier = Modifier.weight(1f),
            label = "结束",
            timeText = formatClipTime(endMs),
            onMinus = { vm.nudgeClipEnd(-1000L) },
            onPlus = { vm.nudgeClipEnd(1000L) },
            enabled = !vm.isClipping
        )
    }
    Spacer(Modifier.height(12.dp))

    // ── 信息 ──
    ClipInfoCard(
        durationMs = duration,
        estimatedBytes = estimatedBytes,
        snapStartMs = if (snapped.first != startMs) snapped.first else -1L,
        snapEndMs = if (snapped.second != endMs) snapped.second else -1L
    )
    Spacer(Modifier.height(16.dp))

    // ── 操作区 ──
    if (!vm.isClipping) {
        PressButton(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = cardMaxWidth()),
            onClick = vm::startClipDownload,
            background = iOSGreen,
            shadowColor = iOSGreen.copy(alpha = 0.3f),
            contentColor = Color.White
        ) {
            Text(
                if (estimatedBytes > 0) "剪辑并下载（约 ${formatFileSize(estimatedBytes)}）"
                else "剪辑并下载",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.25.sp
            )
        }
    } else {
        ClipProgressArea(
            phaseText = vm.clipPhaseText,
            percent = vm.clipPercent,
            onCancel = vm::cancelClip
        )
    }
}

/** 双滑块时间轴：缩略图条 + 选区高亮 + 可拖拽把手 */
@Composable
private fun ClipTimelineStrip(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    thumbs: List<ImageBitmap?>,
    enabled: Boolean,
    onRangeChanged: (Long, Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // pointerInput 不随重组重启，用 rememberUpdatedState 保证手势回调读到最新值
    val currentStartMs by rememberUpdatedState(startMs)
    val currentEndMs by rememberUpdatedState(endMs)
    val currentOnRangeChanged by rememberUpdatedState(onRangeChanged)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        val stripW = maxWidth
        val stripWpx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val startFrac = (startMs.toFloat() / durationMs).coerceIn(0f, 1f)
        val endFrac = (endMs.toFloat() / durationMs).coerceIn(0f, 1f)
        val handleW = 20.dp
        val selStart = stripW * startFrac
        val selWidth = (stripW * (endFrac - startFrac)).coerceAtLeast(handleW)

        fun msAt(x: Float): Long = (x / stripWpx * durationMs).toLong().coerceIn(0L, durationMs)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E))
                .then(
                    if (enabled) Modifier.pointerInput(durationMs) {
                        // 点击时间轴：把手就近移动
                        detectTapGestures { offset ->
                            val t = msAt(offset.x)
                            val distStart = kotlin.math.abs(t - currentStartMs)
                            val distEnd = kotlin.math.abs(currentEndMs - t)
                            if (distStart <= distEnd) currentOnRangeChanged(t, currentEndMs)
                            else currentOnRangeChanged(currentStartMs, t)
                        }
                    } else Modifier
                )
        ) {
            // 缩略图
            Row(modifier = Modifier.fillMaxSize()) {
                if (thumbs.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2C2C2E), Color(0xFF3A3A3C), Color(0xFF2C2C2E))
                            )
                        )
                    )
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

            // 选区外的压暗层
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(selStart)
                    .background(Color.Black.copy(alpha = 0.55f))
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .width(stripW - selStart - selWidth)
                    .background(Color.Black.copy(alpha = 0.55f))
            )

            // 选区边框
            Box(
                Modifier
                    .fillMaxHeight()
                    .absoluteOffset { IntOffset(selStart.toPx().roundToInt(), 0) }
                    .width(selWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
            )

            // 左把手
            ClipHandle(
                modifier = Modifier
                    .fillMaxHeight()
                    .absoluteOffset {
                        IntOffset((selStart.toPx() - handleW.toPx() / 2).roundToInt(), 0)
                    }
                    .width(handleW),
                enabled = enabled,
                onDragStart = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { dragAmount ->
                    currentOnRangeChanged(
                        msAt(startFrac * stripWpx + dragAmount),
                        currentEndMs
                    )
                }
            )
            // 右把手
            ClipHandle(
                modifier = Modifier
                    .fillMaxHeight()
                    .absoluteOffset {
                        IntOffset((selStart.toPx() + selWidth.toPx() - handleW.toPx() / 2).roundToInt(), 0)
                    }
                    .width(handleW),
                enabled = enabled,
                onDragStart = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { dragAmount ->
                    currentOnRangeChanged(
                        currentStartMs,
                        msAt(endFrac * stripWpx + dragAmount)
                    )
                }
            )
        }
    }
}

@Composable
private fun ClipHandle(
    modifier: Modifier,
    enabled: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .then(
                if (enabled) Modifier.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { currentOnDragStart() }
                    ) { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount)
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(3) {
                Box(
                    Modifier
                        .padding(vertical = 2.dp)
                        .size(width = 2.dp, height = 12.dp)
                        .background(Color(0xFF1C1C1E).copy(alpha = 0.55f), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

@Composable
private fun TimeStepperCard(
    modifier: Modifier = Modifier,
    label: String,
    timeText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5FA))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 11.sp, color = iOSSecondary, fontWeight = FontWeight.Medium)
            Text(
                timeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSLabel,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.weight(1f))
        StepperButton("−", enabled, onMinus)
        Spacer(Modifier.width(6.dp))
        StepperButton("+", enabled, onPlus)
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (enabled) iOSBlue.copy(alpha = 0.12f) else Color(0xFFE5E5EA))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) iOSBlue else iOSTertiary
        )
    }
}

@Composable
private fun ClipInfoCard(
    durationMs: Long,
    estimatedBytes: Long,
    snapStartMs: Long,
    snapEndMs: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5FA))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        InfoRow("原视频", formatClipTime(durationMs))
        if (estimatedBytes > 0) {
            Spacer(Modifier.height(4.dp))
            InfoRow("预计下载", "约 ${formatFileSize(estimatedBytes)}")
        }
        if (snapStartMs >= 0 || snapEndMs >= 0) {
            Spacer(Modifier.height(4.dp))
            InfoRow(
                "将按关键帧对齐",
                "${formatClipTime(if (snapStartMs >= 0) snapStartMs else 0L)} - " +
                    formatClipTime(if (snapEndMs >= 0) snapEndMs else 0L),
                valueColor = Color(0xFFFF9500)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = iOSLabel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = iOSSecondary)
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
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
                color = iOSBlue
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
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
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(iOSBlue, iOSBlueLight)))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "取消剪辑",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFF3B30),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onCancel
                )
                .padding(8.dp)
        )
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
