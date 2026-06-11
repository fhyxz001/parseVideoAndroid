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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

/* ─────────────────────────────────── Colors ─────────────────────────────── */

private val BgGradient = Brush.verticalGradient(
    listOf(Color(0xFFF0F4FF), Color(0xFFE8EEFF), Color(0xFFF3EEFF), Color(0xFFFEF0F5))
)
private val PurpleStart = Color(0xFF667EEA)
private val PurpleEnd   = Color(0xFF764BA2)
private val ParseGradient = Brush.horizontalGradient(listOf(PurpleStart, PurpleEnd))
private val ParseDisabledGradient = Brush.horizontalGradient(
    listOf(Color(0xFFC5CDF0), Color(0xFFD0C5E0))
)
private val GreenStart  = Color(0xFF43E97B)
private val GreenEnd    = Color(0xFF38F9D7)
private val PinkStart   = Color(0xFFFA709A)
private val YellowEnd   = Color(0xFFFEE140)
private val BlueStart   = Color(0xFF4FACFE)
private val BlueEnd     = Color(0xFF00F2FE)

/* ───────────────────────────────── Screen ───────────────────────────────── */

@Composable
fun VideoParserScreen(vm: MainViewModel) {
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

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
        }

        // Global loading overlay
        if (vm.isParsing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("解析中...", color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

/* ───────────────────────────────── Input card ───────────────────────────── */

@Composable
fun InputCard(videoUrl: String, onUrlChanged: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) PurpleStart else Color.Transparent,
        label = "border"
    )
    val bgColor by animateColorAsState(
        if (focused) Color.White else Color(0xFFF5F6FA),
        label = "bg"
    )

    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔗", fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
            BasicTextField(
                value = videoUrl,
                onValueChange = onUrlChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .onFocusChanged { focused = it.isFocused },
                textStyle = TextStyle(color = Color(0xFF333333), fontSize = 15.sp),
                cursorBrush = SolidColor(PurpleStart),
                singleLine = true,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                        if (videoUrl.isEmpty()) {
                            Text("请输入或粘贴视频链接", color = Color(0xFFC0C4CC), fontSize = 15.sp)
                        }
                        inner()
                    }
                }
            )
        }
    }
}

/* ───────────────────────────────── Buttons ──────────────────────────────── */

@Composable
fun ButtonRow(
    isParsing: Boolean,
    hasUrl: Boolean,
    onPaste: (Context) -> Unit,
    onParse: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PressButton(
            modifier = Modifier.weight(1f),
            onClick = { onPaste(context) },
            background = Brush.horizontalGradient(listOf(Color(0xFFF8F9FD), Color(0xFFEEF0F7))),
            shadowColor = Color.Black.copy(alpha = 0.05f)
        ) {
            Text("粘贴链接", color = Color(0xFF555555), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        val enabled = hasUrl && !isParsing
        PressButton(
            modifier = Modifier.weight(1f),
            onClick = { if (enabled) onParse() },
            background = if (enabled) ParseGradient else ParseDisabledGradient,
            shadowColor = PurpleStart.copy(alpha = if (enabled) 0.35f else 0f)
        ) {
            Text(
                if (isParsing) "解析中..." else "开始解析",
                color = Color.White.copy(alpha = if (enabled) 1f else 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

/* ────────────────────────────── Result section ──────────────────────────── */

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
    Column(modifier = Modifier.fillMaxWidth()) {
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
        if (isDownloading) {
            Spacer(Modifier.height(16.dp))
            ProgressSection(downloadPercent)
        }
    }
}

@Composable
fun VideoCard(data: VideoData) {
    GlassCard {
        Box {
            Column {
                if (data.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = data.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.height(14.dp))
                }
                if (data.title.isNotBlank()) {
                    Text(
                        text = data.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color(0xFF1A1A2E),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(10.dp))
                }
                data.author?.let { author ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PurpleStart, PurpleEnd))),
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
                        Text("@${author.name}", color = Color(0xFF999999), fontSize = 13.sp)
                    }
                }
            }
            // Success tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(GreenStart, GreenEnd)))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("✓ 解析成功", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ─────────────────────────────── Action grid ────────────────────────────── */

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
            label = if (isDownloading) "下载中" else "下载",
            iconRes = R.drawable.ic_action_1,
            gradient = Brush.linearGradient(listOf(GreenStart, GreenEnd)),
            shadowColor = GreenStart.copy(0.3f),
            disabled = isDownloading,
            onClick = onDownload
        ),
        ActionItem(
            label = "复制长链",
            iconRes = R.drawable.ic_action_2,
            gradient = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
            shadowColor = PurpleStart.copy(0.3f),
            disabled = false,
            onClick = onCopyLongLink
        ),
        ActionItem(
            label = if (isGettingShortLink) "获取中" else "复制短链",
            iconRes = R.drawable.ic_action_3,
            gradient = Brush.linearGradient(listOf(PinkStart, YellowEnd)),
            shadowColor = PinkStart.copy(0.3f),
            disabled = isGettingShortLink,
            onClick = onCopyShortLink
        ),
        ActionItem(
            label = "分享",
            iconRes = R.drawable.ic_action_4,
            gradient = Brush.linearGradient(listOf(BlueStart, BlueEnd)),
            shadowColor = BlueStart.copy(0.3f),
            disabled = false,
            onClick = onShare
        )
    )

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
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "scale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(0.5f), RoundedCornerShape(16.dp))
            .then(
                if (!item.disabled)
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = item.onClick
                    )
                else Modifier
            )
            .then(if (item.disabled) Modifier.graphicsLayer { alpha = 0.45f } else Modifier)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
            Spacer(Modifier.height(8.dp))
            Text(item.label, fontSize = 12.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
        }
    }
}

/* ──────────────────────────────── Progress ─────────────────────────────── */

@Composable
fun ProgressSection(percent: Int) {
    GlassCard {
        Column {
            Text(
                "正在下载视频...",
                color = PurpleStart,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEEF0F5))
                ) {
                    val animatedPercent by animateFloatAsState(
                        percent / 100f, animationSpec = tween(300), label = "prog"
                    )
                    val shimmer = rememberInfiniteTransition(label = "shimmer")
                    val offset by shimmer.animateFloat(
                        0f, 1f,
                        infiniteRepeatable(tween(1500, easing = LinearEasing)),
                        label = "offset"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedPercent)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PurpleStart, PurpleEnd, PurpleStart),
                                    startX = offset * 300f,
                                    endX = offset * 300f + 300f
                                )
                            )
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "$percent%",
                    color = PurpleEnd,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.widthIn(min = 40.dp)
                )
            }
        }
    }
}

/* ──────────────────────────────── Empty state ───────────────────────────── */

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFF0F4FF), Color(0xFFF3EEFF)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🎬", fontSize = 48.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "粘贴短视频链接",
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = Color(0xFF333333)
        )
        Spacer(Modifier.height(8.dp))
        Text("一键解析无水印视频", fontSize = 13.sp, color = Color(0xFFBBBBBB))
    }
}

/* ─────────────────────────────── GlassCard ─────────────────────────────── */

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(0.6f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        content()
    }
}

/* ─────────────────────────────── PressButton ───────────────────────────── */

@Composable
fun PressButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    background: Brush,
    shadowColor: Color,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "scale")

    Box(
        modifier = modifier
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
