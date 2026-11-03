@file:OptIn(ExperimentalMaterial3Api::class)

package com.voicelocker.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
import com.voicelocker.app.data.local.Store
import com.voicelocker.app.data.model.languages
import com.voicelocker.app.data.model.lockThemes
import com.voicelocker.app.ui.theme.Blue
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
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Scaffold(
        containerColor = c.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium) },
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
        },
        bottomBar = {
            if (bottom != null) Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding(), Alignment.TopCenter) {
                Column(Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().padding(16.dp), content = bottom)
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
    icon: ImageVector? = null
) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = when (kind) {
        Btn.Primary -> c.primary to Color.White
        Btn.White -> c.surface to c.onSurface
        Btn.Danger -> c.error to Color.White
        Btn.Outline -> c.surface to c.primary
    }
    Button(
        onClick,
        modifier.fillMaxWidth().height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = if (kind == Btn.Outline) BorderStroke(1.dp, c.primary) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = c.primaryContainer,
            disabledContentColor = c.primary.copy(alpha = .5f)
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Muted(text: String, size: Int = 11, modifier: Modifier = Modifier, align: TextAlign? = null) =
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = size.sp, textAlign = align)

@Composable
fun StepDots(step: Int, error: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val col2 = if (error) c.error else c.primary
    @Composable fun dot(n: Int, col: Color) {
        val filled = step >= n
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(if (filled) col else c.surface).border(1.5.dp, col, CircleShape),
            Alignment.Center
        ) {
            if (filled) Text("$n", color = Color.White, fontSize = 12.sp)
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.Center, Alignment.CenterVertically) {
        dot(1, c.primary)
        Box(Modifier.width(44.dp).height(2.dp).background(if (error && step >= 2) col2 else c.primary))
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
    Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(4) { i ->
            val on = i < filled
            val col = if (error) c.error else if (glass) Color.White else c.primary
            val digit = digits?.getOrNull(i)?.toString()

            if (showPin && on && digit != null) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (glass) Color.White.copy(0.25f) else c.primaryContainer)
                        .border(1.5.dp, col, CircleShape),
                    Alignment.Center
                ) {
                    Text(
                        digit,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (glass) Color.White else c.onSurface
                    )
                }
            } else {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (on) col else Color.Transparent)
                        .border(1.5.dp, if (glass) Color.White.copy(.7f) else c.primaryContainer, CircleShape)
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
    val c = MaterialTheme.colorScheme
    val compact = LocalConfiguration.current.screenHeightDp < 680
    val size = if (compact) 54.dp else 66.dp
    @Composable fun key(label: String?, onClick: () -> Unit) {
        if (label == null) { Spacer(Modifier.size(size)); return }
        Box(
            Modifier.size(size).clip(CircleShape).background(if (glass) Color.White.copy(.25f) else c.primaryContainer).clickable(onClick = onClick),
            Alignment.Center
        ) {
            when (label) {
                "x" -> Icon(Icons.Filled.Close, "Delete", tint = if (glass) Color.White else c.onSurface)
                "undo" -> Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (glass) Color.White else c.onSurface)
                else -> Text(label, fontSize = 24.sp, color = if (glass) Color.White else c.onSurface)
            }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) { row.forEach { d -> key(d) { onDigit(d) } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
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
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 4 else 2
    items.chunked(cols).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { i ->
                ThemeBg(i, Modifier.weight(1f).aspectRatio(.75f).clickable { onClick(i) }, RoundedCornerShape(16.dp)) {
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
fun SuccessDialog(msg: String, onDone: () -> Unit) {
    LaunchedEffect(msg) { delay(1800); onDone() }
    val cols = listOf(Color(0xFFE91E63), Color(0xFF009688), Color(0xFFFFC107), Blue, Color(0xFF9C27B0))
    Dialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(130.dp), Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        repeat(14) { i ->
                            val a = i * 2 * PI / 14
                            val r = if (i % 2 == 0) 58.dp.toPx() else 47.dp.toPx()
                            drawCircle(cols[i % 5], 3.5.dp.toPx(), center + Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat()))
                        }
                    }
                    Box(Modifier.size(68.dp).clip(CircleShape).background(Success), Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Congratulations!", color = Success, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(msg, textAlign = TextAlign.Center)
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
            Page("Choose Language", onBack = onDismiss, scroll = false, actions = {
                if (sel != selected) IconButton({ onConfirm(sel) }) { Icon(Icons.Filled.Check, "Confirm", tint = c.primary) }
            }) {
                OutlinedTextField(
                    q, { q = it }, Modifier.fillMaxWidth().padding(bottom = 12.dp), singleLine = true,
                    placeholder = { Text("Search 100+ languages...") }, trailingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = c.surface, unfocusedContainerColor = c.surface, unfocusedBorderColor = Color.Transparent),
                )
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered.size, key = { filtered[it].tag + "_" + filtered[it].name }) { idx ->
                        val l = filtered[idx]
                        RowCard(selected = sel == l.name, onClick = { sel = l.name }) {
                            Text(l.flag, fontSize = 22.sp)
                            Spacer(Modifier.width(16.dp))
                            Text(l.name, Modifier.weight(1f), fontSize = 14.sp)
                            RadioButton(sel == l.name, null)
                        }
                    }
                }
            }
        }
    }
}

