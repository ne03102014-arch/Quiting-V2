package com.quiting.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.quiting.app.data.VpnProfile
import com.quiting.app.vpn.TunnelService
import com.quiting.app.vpn.TunnelState
import com.quiting.app.viewmodel.AppViewModel
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    CONFIGS("Configs", Icons.Filled.Menu),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@Composable
fun AppRoot(vm: AppViewModel) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var showAdd by remember { mutableStateOf(false) }
    val msg = vm.message
    val context = LocalContext.current
    val status by TunnelState.status.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val sel = vm.selected
        if (result.resultCode == Activity.RESULT_OK && sel != null) {
            TunnelService.start(context, sel.raw)
        } else {
            TunnelState.set(TunnelState.Status.DISCONNECTED, "VPN permission was not granted")
        }
    }

    val onConnect: () -> Unit = {
        if (status == TunnelState.Status.CONNECTED || status == TunnelState.Status.CONNECTING) {
            TunnelService.stop(context)
        } else {
            val sel = vm.selected
            if (sel == null) {
                vm.showMessage("Add a configuration first")
            } else {
                val prepare = VpnService.prepare(context)
                if (prepare != null) vpnLauncher.launch(prepare) else TunnelService.start(context, sel.raw)
            }
        }
    }

    LaunchedEffect(msg) {
        if (msg != null) {
            delay(3500)
            vm.dismissMessage()
        }
    }

    StyledBackground(vm.style) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { BottomBar(current = tab, style = vm.style, onSelect = { tab = it }) }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (tab) {
                    Tab.HOME -> HomeScreen(vm, status, onConnect)
                    Tab.CONFIGS -> ConfigsScreen(vm) { showAdd = true }
                    Tab.SETTINGS -> SettingsScreen(vm)
                }
                if (msg != null) {
                    MessageBanner(
                        text = msg,
                        style = vm.style,
                        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp),
                        onDismiss = { vm.dismissMessage() }
                    )
                }
            }
        }
    }

    if (showAdd) AddDialog(vm) { showAdd = false }
}

@Composable
private fun BottomBar(current: Tab, style: AppStyle, onSelect: (Tab) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .glassCard(style, RoundedCornerShape(28.dp))
    ) {
        NavigationBar(containerColor = Color.Transparent) {
            Tab.values().forEach { t ->
                NavigationBarItem(
                    selected = current == t,
                    onClick = { onSelect(t) },
                    icon = { Icon(t.icon, contentDescription = t.label) },
                    label = { Text(t.label) }
                )
            }
        }
    }
}

@Composable
private fun MessageBanner(text: String, style: AppStyle, modifier: Modifier, onDismiss: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .glassCard(style, RoundedCornerShape(16.dp))
            .clickable(onClick = onDismiss)
            .padding(14.dp)
    )
}

@Composable
private fun HomeScreen(vm: AppViewModel, status: TunnelState.Status, onConnect: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val sel = vm.selected
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Quiting", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            when (status) {
                TunnelState.Status.CONNECTED -> "Protected"
                TunnelState.Status.CONNECTING -> "Connecting..."
                TunnelState.Status.ERROR -> TunnelState.lastError ?: "Connection failed"
                TunnelState.Status.DISCONNECTED -> "Not connected"
            },
            color = if (status == TunnelState.Status.ERROR) c.error else c.onSurfaceVariant
        )

        ConnectOrb(connected = status == TunnelState.Status.CONNECTED, onClick = onConnect)

        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(vm.style, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Text("Active configuration", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            if (sel == null) {
                Text("No configuration yet. Add one in the Configs tab.")
            } else {
                Text(sel.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${sel.protocol.label} · ${sel.host}:${sel.port}",
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ConnectOrb(connected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val accent by animateColorAsState(
        targetValue = if (connected) c.tertiary else c.primary,
        label = "accent"
    )
    val spinTransition = rememberInfiniteTransition(label = "orb")
    val spin by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "spin"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(220.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                drawCircle(
                    brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.4f), Color.Transparent), center = center, radius = w / 2),
                    radius = w / 2
                )
                for (i in 0..2) {
                    val inset = i * 16.dp.toPx()
                    drawArc(
                        color = accent.copy(alpha = 0.35f + 0.2f * i),
                        startAngle = spin * (1f + 0.5f * i) + i * 110f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(w - 2 * inset, w - 2 * inset),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Box(
                Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accent, accent.copy(alpha = 0.35f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (connected) Icons.Filled.Check else Icons.Filled.PlayArrow,
                    contentDescription = if (connected) "Disconnect" else "Connect",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Text(if (connected) "Disconnect" else "Connect", style = MaterialTheme.typography.titleMedium, color = accent)
    }
}

@Composable
private fun ConfigsScreen(vm: AppViewModel, onAdd: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Configurations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            if (vm.profiles.isEmpty()) {
                item {
                    Text(
                        "No configurations yet. Tap + to import links or a subscription.",
                        color = c.onSurfaceVariant
                    )
                }
            }
            items(vm.profiles, key = { it.id }) { p ->
                ProfileRow(
                    profile = p,
                    selected = p.id == vm.selected?.id,
                    style = vm.style,
                    onSelect = { vm.select(p.id) },
                    onFavorite = { vm.toggleFavorite(p.id) },
                    onDelete = { vm.remove(p.id) }
                )
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
    }
}

@Composable
private fun ProfileRow(
    profile: VpnProfile,
    selected: Boolean,
    style: AppStyle,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val selectedBorder = if (selected) Modifier.border(2.dp, c.primary, shape) else Modifier
    Row(
        Modifier
            .fillMaxWidth()
            .glassCard(style, shape)
            .then(selectedBorder)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                profile.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${profile.protocol.label} · ${profile.host}:${profile.port}",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onFavorite) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Favorite",
                tint = if (profile.favorite) c.tertiary else c.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = c.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var links by remember { mutableStateOf("") }
    var subscription by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = links,
                    onValueChange = { links = it },
                    label = { Text("Links: vless, vmess, trojan, ss") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { vm.importText(links) },
                    enabled = links.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import links") }

                OutlinedTextField(
                    value = subscription,
                    onValueChange = { subscription = it },
                    label = { Text("Subscription URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { vm.importSubscription(subscription.trim()) },
                    enabled = subscription.trim().startsWith("http") && !vm.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (vm.busy) "Loading..." else "Import subscription") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun SettingsScreen(vm: AppViewModel) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Text("Interface style", style = MaterialTheme.typography.titleSmall, color = c.onSurfaceVariant)
        AppStyle.values().forEach { s ->
            val selected = s == vm.style
            val shape = RoundedCornerShape(20.dp)
            val selectedBorder = if (selected) Modifier.border(2.dp, c.primary, shape) else Modifier
            Row(
                Modifier
                    .fillMaxWidth()
                    .glassCard(vm.style, shape)
                    .then(selectedBorder)
                    .clickable { vm.chooseStyle(s) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(s.previewBrush()))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.label, style = MaterialTheme.typography.titleMedium)
                    Text(s.hint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = c.primary)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("Theme", style = MaterialTheme.typography.titleSmall, color = c.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { m ->
                FilterChip(
                    selected = vm.themeMode == m,
                    onClick = { vm.chooseMode(m) },
                    label = { Text(m.label) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("About", style = MaterialTheme.typography.titleSmall, color = c.onSurfaceVariant)
        Text(
            "Quiting imports VPN configurations and runs them through a VpnService. The sing-box core (libbox.aar) must be added to app/libs before Connect can open a tunnel.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant
        )
    }
}

private fun AppStyle.previewBrush(): Brush = when (this) {
    AppStyle.MATERIAL_YOU -> Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFFEADDFF)))
    AppStyle.LIQUID_GLASS -> Brush.linearGradient(listOf(Color(0xFF3A5BD9), Color(0xFF7FE7D9), Color(0xFFD6A8FF)))
    AppStyle.MINIMAL -> Brush.linearGradient(listOf(Color.Black, Color.White))
    AppStyle.NEON -> Brush.linearGradient(listOf(Color(0xFF3EF0FF), Color(0xFFFF3EC8)))
}
