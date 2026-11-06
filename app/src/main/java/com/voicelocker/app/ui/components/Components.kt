@file:OptIn(ExperimentalMaterial3Api::class)

package com.voicelocker.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voicelocker.ads.AppAdaptiveBannerTop
import com.voicelocker.ads.AppNativeAdView
import com.voicelocker.ads.BillingManager
import com.voicelocker.app.R
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.languages
import com.voicelocker.app.data.model.lockThemes
import com.voicelocker.app.ui.theme.Blue
import com.voicelocker.app.ui.theme.InterFontFamily
import com.voicelocker.app.ui.theme.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

val MaxContentWidth = 560.dp

/** Standard screen: top bar, centered max-width column (tablet friendly), optional pinned bottom area. */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    scroll: Boolean = true,
    adScreenKey: String? = null,
    nativeTemplate: Int = 1,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Scaffold(
        containerColor = c.background,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.background)
            ) {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily) },
                    navigationIcon = {
                        if (onBack != null) IconButton(onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = c.background,
                        titleContentColor = c.onBackground
                    ),
                )
                if (adScreenKey != null) {
                    AppAdaptiveBannerTop(adScreenKey)
                }
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isCompact = LocalConfiguration.current.screenHeightDp < 820
                if (bottom != null) {
                    Column(
                        Modifier
                            .widthIn(max = MaxContentWidth)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = if (isCompact) 2.dp else 4.dp),
                        content = bottom
                    )
                }
                if (adScreenKey != null) {
                    AppNativeAdView(
                        screenKey = adScreenKey,
                        template = nativeTemplate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        isBottom = false
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad), Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = MaxContentWidth).fillMaxSize()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 16.dp),
                content = content,
            )
        }
    }
}

@Composable
fun TabColumn(content: @Composable ColumnScope.() -> Unit) = Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
    Column(
        Modifier.widthIn(max = MaxContentWidth).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        content = content
    )
}

@Composable
fun RowCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    pad: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val m = modifier.fillMaxWidth().padding(bottom = 10.dp)
    val border = if (selected) BorderStroke(1.dp, c.primary) else null
    val inner: @Composable () -> Unit = {
        Row(Modifier.padding(pad), verticalAlignment = Alignment.CenterVertically, content = content)
    }
    if (onClick != null) Surface(onClick, m, shape = RoundedCornerShape(14.dp), color = c.surface, border = border, content = inner)
    else Surface(m, shape = RoundedCornerShape(14.dp), color = c.surface, border = border, content = inner)
}

@Composable
fun ColCard(
    modifier: Modifier = Modifier,
    center: Boolean = false,
    onClick: (() -> Unit)? = null,
    pad: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val m = modifier.fillMaxWidth().padding(bottom = 10.dp)
    val inner: @Composable () -> Unit = {
        Column(
            Modifier.fillMaxWidth().padding(pad),
            horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
            content = content
        )
    }
    if (onClick != null) Surface(onClick, m, shape = RoundedCornerShape(14.dp), color = c.surface, content = inner)
    else Surface(m, shape = RoundedCornerShape(14.dp), color = c.surface, content = inner)
}

enum class Btn { Primary, White, Danger, Outline }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: Btn = Btn.Primary,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = when (kind) {
        Btn.Primary -> c.primary to Color.White
        Btn.White -> c.surface to c.onSurface
        Btn.Danger -> c.error to Color.White
        Btn.Outline -> c.surface to c.primary
    }
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        enabled = enabled,
        shape = shape,
        border = if (kind == Btn.Outline) BorderStroke(1.dp, c.primary) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = if (kind == Btn.Primary) Color(0xFFBFE0FE) else c.primaryContainer,
            disabledContentColor = if (kind == Btn.Primary) Color.White else c.primary.copy(alpha = .5f)
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
    }
}

@Composable
fun SleekSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackWidth = 50.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val padding = 3.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0E1320)
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF2A3550) else Color(0xFFE5E2E7),
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onCheckedChange(!checked)
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun Muted(text: String, size: Int = 11, modifier: Modifier = Modifier, align: TextAlign? = null) =
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = size.sp, textAlign = align, fontFamily = InterFontFamily)

@Composable
fun StepDots(step: Int, error: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val col2 = if (error) c.error else c.primary
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val isSmall = screenHeight < 820
    @Composable fun dot(n: Int, col: Color) {
        val filled = step >= n || (error && n == 2)
        Box(
            Modifier.size(if (isSmall) 22.dp else 24.dp).clip(CircleShape).background(if (filled) col else c.surface).border(1.5.dp, col, CircleShape),
            Alignment.Center
        ) {
            if (filled) Text("$n", color = Color.White, fontSize = if (isSmall) 11.sp else 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = if (isSmall) 4.dp else 8.dp), Arrangement.Center, Alignment.CenterVertically) {
        dot(1, c.primary)
        Box(Modifier.width(if (isSmall) 36.dp else 44.dp).height(2.dp).background(c.primary))
        dot(2, col2)
    }
}

@Composable
fun MicButton(size: Dp, ring: Color, fill: Color, onClick: (() -> Unit)? = null, icon: ImageVector = Icons.Filled.Mic) {
    Box(
        Modifier.size(size * 1.35f).clip(CircleShape).background(ring).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        Alignment.Center
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(fill), Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * .5f))
        }
    }
}

@Composable
fun PinDots(
    filled: Int,
    error: Boolean = false,
    glass: Boolean = false,
    digits: String? = null,
    showPin: Boolean = false
) {
    val c = MaterialTheme.colorScheme
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val isSmall = screenHeight < 820
    val isVerySmall = screenHeight < 680
    val dotSize = if (isVerySmall) 18.dp else if (isSmall) 20.dp else 24.dp
    Row(
        Modifier.padding(vertical = if (isVerySmall) 2.dp else if (isSmall) 4.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isSmall) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { i ->
            val on = i < filled
            val col = if (error) c.error else if (glass) Color.White else c.primary
            val digit = digits?.getOrNull(i)?.toString()

            if (on) {
                Box(
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(if (glass) Color.White.copy(0.25f) else col),
                    Alignment.Center
                ) {
                    if ((showPin || error) && digit != null) {
                        Text(
                            text = digit,
                            fontSize = if (isVerySmall) 11.sp else if (isSmall) 12.sp else 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            } else {
                Box(
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(1.5.dp, if (glass) Color.White.copy(.7f) else col, CircleShape)
                )
            }
        }
    }
}

@Composable
fun NumPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    glass: Boolean = false,
    onUndo: (() -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val isCompact = screenHeight < 820
    val isSmall = screenHeight < 720
    val isVerySmall = screenHeight < 660

    val size = when {
        isVerySmall -> 44.dp
        isSmall -> 48.dp
        isCompact -> 52.dp
        else -> 56.dp
    }
    val spacingY = when {
        isVerySmall -> 4.dp
        isSmall -> 5.dp
        isCompact -> 6.dp
        else -> 10.dp
    }
    val spacingX = when {
        isVerySmall -> 16.dp
        isSmall -> 18.dp
        isCompact -> 22.dp
        else -> 24.dp
    }
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0E1320)
    val buttonBg = if (glass) Color.White.copy(.25f) else if (isDark) Color(0xFF232C44) else Color(0xFFCCE8FF)
    val contentCol = if (glass) Color.White else if (isDark) Color(0xFFEEF2F8) else Color(0xFF1E2022)

    @Composable fun key(label: String?, onClick: () -> Unit) {
        if (label == null) { Spacer(Modifier.size(size)); return }
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(buttonBg)
                .clickable(onClick = onClick),
            Alignment.Center
        ) {
            when (label) {
                "x" -> Icon(Icons.Filled.Close, "Delete", tint = contentCol, modifier = Modifier.size(if (isVerySmall) 18.dp else if (isSmall) 19.dp else 22.dp))
                "undo" -> Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = contentCol)
                else -> Text(label, fontSize = if (isVerySmall) 16.sp else if (isSmall) 18.sp else 20.sp, color = contentCol, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal)
            }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacingY)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacingX)) { row.forEach { d -> key(d) { onDigit(d) } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacingX)) {
            key("x", onDelete)
            key("0") { onDigit("0") }
            if (onUndo != null) {
                key("undo", onUndo)
            } else {
                key(null) {}
            }
        }
    }
}

/** 3x3 pattern pad. Calls onComplete with node order when the finger lifts. Scales to available width. */
@Composable
fun PatternPad(onComplete: (List<Int>) -> Unit, error: Boolean = false, glass: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val sel = remember { mutableStateListOf<Int>() }
    var cur by remember { mutableStateOf<Offset?>(null) }
    val scope = rememberCoroutineScope()
    val cb by rememberUpdatedState(onComplete)
    BoxWithConstraints(Modifier.fillMaxWidth(), Alignment.Center) {
        val s = minOf(maxWidth, 300.dp)
        val active = if (error) c.error else if (glass) Color.White else c.primary
        val idle = if (glass) Color.White.copy(.25f) else c.primaryContainer
        Canvas(Modifier.size(s).pointerInput(Unit) {
            fun pos(i: Int) = Offset((i % 3 + .5f) * size.width / 3, (i / 3 + .5f) * size.height / 3)
            fun hit(p: Offset) {
                cur = p
                for (i in 0..8) if ((pos(i) - p).getDistance() < size.width / 9f && i !in sel) sel.add(i)
            }
            detectDragGestures(
                onDragStart = { sel.clear(); hit(it) },
                onDrag = { ch, _ -> hit(ch.position) },
                onDragEnd = { cur = null; cb(sel.toList()); scope.launch { delay(700); sel.clear() } },
                onDragCancel = { cur = null; sel.clear() },
            )
        }) {
            fun pos(i: Int) = Offset((i % 3 + .5f) * size.width / 3, (i / 3 + .5f) * size.height / 3)
            val w = 8.dp.toPx()
            for (k in 0 until sel.size - 1) drawLine(active, pos(sel[k]), pos(sel[k + 1]), w, StrokeCap.Round)
            val cp = cur
            if (cp != null && sel.isNotEmpty()) drawLine(active, pos(sel.last()), cp, w, StrokeCap.Round)
            for (i in 0..8) drawCircle(if (i in sel) active else idle, size.width / 13.5f, pos(i))
        }
    }
}

@Composable
fun Waveform(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, active: Boolean = true, bars: Int = 32, height: Dp = 48.dp) {
    val t by rememberInfiniteTransition(label = "wave").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "p")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val bw = size.width / (bars * 2f)
        for (i in 0 until bars) {
            val amp = if (active) .2f + .8f * abs(sin(t + i * .7f) * cos(i * .35f)) else .2f + .5f * abs(sin(i * 1.3f))
            val h = size.height * amp
            drawRoundRect(color, Offset(i * 2 * bw + bw / 2, (size.height - h) / 2), Size(bw, h), CornerRadius(bw / 2))
        }
    }
}

/** Wallpaper (res/drawable/theme{n}) with gradient fallback. */
@Composable
fun ThemeBg(i: Int, modifier: Modifier = Modifier, shape: Shape = RectangleShape, content: @Composable BoxScope.() -> Unit = {}) {
    val t = lockThemes[i % lockThemes.size]
    val ctx = LocalContext.current
    val res = remember(i) { ctx.resources.getIdentifier("theme${i % lockThemes.size + 1}", "drawable", ctx.packageName) }
    Box(modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(t.c1), Color(t.c2))))) {
        if (res != 0) Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        content()
    }
}

@Composable
fun ThemeGrid(items: List<Int>, onClick: (Int) -> Unit) {
    val ctx = LocalContext.current
    val isPremium = BillingManager.getInstance(ctx).isPremiumUser()
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 4 else 2
    items.chunked(cols).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { i ->
                val isUnlocked = Store.isThemeUnlocked(i, isPremium)
                ThemeBg(i, Modifier.weight(1f).aspectRatio(.75f).clickable { onClick(i) }, RoundedCornerShape(16.dp)) {
                    if (!isUnlocked) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xB3000000)),
                            Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_crown_home),
                                contentDescription = "Premium",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    if (Store.themeIndex == i) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(Blue), Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun ThemeHorizontalRow(
    items: List<Int> = lockThemes.indices.toList(),
    onClick: (Int) -> Unit
) {
    val ctx = LocalContext.current
    val isPremium = BillingManager.getInstance(ctx).isPremiumUser()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(items) { i ->
            val isUnlocked = Store.isThemeUnlocked(i, isPremium)
            ThemeBg(
                i = i,
                modifier = Modifier
                    .width(115.dp)
                    .aspectRatio(0.72f)
                    .clickable { onClick(i) },
                shape = RoundedCornerShape(16.dp)
            ) {
                if (!isUnlocked) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xB3000000)),
                        Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_crown_home),
                            contentDescription = "Premium",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                if (Store.themeIndex == i) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Blue),
                        Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessDialog(msg: String, onDone: () -> Unit) {
    LaunchedEffect(msg) { delay(2200); onDone() }
    val colors = listOf(
        Color(0xFFE91E63),
        Color(0xFF00BFA5),
        Color(0xFFFFB300),
        Color(0xFFFF6D00),
        Color(0xFF03A9F4)
    )
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0E1320)
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF1B2234) else Color.White,
            border = if (isDark) BorderStroke(1.dp, Color(0xFF2E3852)) else null,
            modifier = Modifier
                .width(300.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cX = size.width / 2
                        val cY = size.height / 2
                        repeat(16) { i ->
                            val angle = (i * 2 * PI / 16).toFloat()
                            val dist = if (i % 2 == 0) 54.dp.toPx() else 44.dp.toPx()
                            val px = cX + cos(angle) * dist
                            val py = cY + sin(angle) * dist
                            val col = colors[i % colors.size]
                            if (i % 3 == 0) {
                                drawCircle(col, radius = 3.dp.toPx(), center = Offset(px, py))
                            } else {
                                rotate(degrees = (angle * 180f / PI).toFloat() + 45f, pivot = Offset(px, py)) {
                                    drawRoundRect(
                                        color = col,
                                        topLeft = Offset(px - 4.dp.toPx(), py - 1.5.dp.toPx()),
                                        size = Size(8.dp.toPx(), 3.dp.toPx()),
                                        cornerRadius = CornerRadius(1.5.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2BB734)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Congratulations!",
                    color = Color(0xFF2BB734),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = msg,
                    textAlign = TextAlign.Center,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E2022),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun LanguageDialog(selected: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    var sel by remember { mutableStateOf(selected) }
    var q by remember { mutableStateOf("") }
    val filtered = remember(q) {
        if (q.isBlank()) languages else languages.filter { it.name.contains(q, true) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = c.background) {
            Page(
                title = "Choose Language",
                onBack = onDismiss,
                scroll = false,
                actions = {
                    if (sel.isNotBlank()) {
                        IconButton(onClick = { onConfirm(sel) }) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Confirm",
                                tint = c.primary
                            )
                        }
                    }
                }
            ) {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Search language",
                            fontFamily = InterFontFamily,
                            fontSize = 15.sp,
                            color = Color(0xFF9DA3B4)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF9DA3B4),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = c.surface,
                        unfocusedContainerColor = c.surface,
                        focusedBorderColor = c.primary,
                        unfocusedBorderColor = c.outline,
                        cursorColor = c.primary,
                        focusedTextColor = c.onSurface,
                        unfocusedTextColor = c.onSurface
                    )
                )

                Spacer(Modifier.height(14.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered.size, key = { filtered[it].tag + "_" + filtered[it].name }) { idx ->
                        val l = filtered[idx]
                        val isSelected = sel == l.name
                        Surface(
                            onClick = { sel = l.name },
                            shape = RoundedCornerShape(14.dp),
                            color = c.surface,
                            border = if (isSelected) BorderStroke(1.5.dp, c.primary) else BorderStroke(1.dp, c.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = l.flag,
                                    fontSize = 22.sp
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = l.name,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = c.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier.size(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(c.primary)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, Color(0xFFC7CCD8), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                AppNativeAdView("language_screen", template = 3, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
            }
        }
    }
}

@Composable
fun ExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val isDark = c.background == Color(0xFF0E1320)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = c.surface,
            border = BorderStroke(1.dp, c.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Exit Icon Badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF2C1E26) else Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Exit",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Exit Voice Screen Locker?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center,
                    color = c.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to close and exit the application?",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel / Stay Button
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF232C44) else Color(0xFFF1F5F9),
                            contentColor = if (isDark) Color(0xFFEEF2F8) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFontFamily
                        )
                    }

                    // Exit Button
                    Button(
                        onClick = onExit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Exit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


