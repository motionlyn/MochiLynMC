package com.mcphone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { Screen() } } }
    }
}

@Composable
fun Screen() {
    val ctx = LocalContext.current
    val status by ServerManager.status.collectAsState()
    val running by ServerManager.running.collectAsState()
    val log by ServerManager.log.collectAsState()
    var type by remember { mutableStateOf(ServerType.BEDROCK) }
    var ram by remember { mutableStateOf(1024f) }
    var cmd by remember { mutableStateOf("") }
    var tunnelOn by remember { mutableStateOf(true) }
    val claim by TunnelManager.claimUrl.collectAsState()
    val addr by TunnelManager.address.collectAsState()
    val tStatus by TunnelManager.status.collectAsState()
    val uri = LocalUriHandler.current
    val clip = LocalClipboardManager.current
    val listState = rememberLazyListState()
    LaunchedEffect(log.size) { if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1) }

    Column(Modifier.padding(16.dp).statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("MCPhone", style = MaterialTheme.typography.headlineMedium)
        Text("Jadikan HP kamu server Minecraft", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ServerType.values().forEach {
                FilterChip(selected = type == it, enabled = !running, onClick = { type = it }, label = { Text(it.label) })
            }
        }
        Text("RAM server: ${ram.toInt()} MB")
        Slider(value = ram, onValueChange = { ram = it }, valueRange = 512f..3072f, steps = 4, enabled = !running)
        Button(onClick = {
            if (running) { ServerManager.stop(); ServerService.stop(ctx) }
            else { ServerService.start(ctx); ServerManager.start(ctx, type, ram.toInt()); if (tunnelOn) TunnelManager.start(ctx) }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (running) "Hentikan Server" else "Mulai Server") }
        Text(status, style = MaterialTheme.typography.titleSmall)
        Text("Teman 1 WiFi: ${ServerManager.localIp()}:${type.port}", fontSize = 13.sp)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(tunnelOn, { tunnelOn = it }, enabled = !running)
            Spacer(Modifier.width(8.dp)); Text("Izinkan teman dari luar WiFi / data seluler")
        }
        if (running && tunnelOn) {
            Text(tStatus, fontSize = 13.sp)
            claim?.let { Button({ uri.openUri(it) }) { Text("Tautkan akun playit.gg (sekali saja)") } }
            addr?.let { a -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Alamat publik: $a", Modifier.weight(1f), fontSize = 13.sp)
                TextButton({ clip.setText(AnnotatedString(a)) }) { Text("Salin") } } }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
            items(log) { Text(it, fontSize = 11.sp) }
        }
        if (running) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(cmd, { cmd = it }, Modifier.weight(1f), label = { Text("Perintah (mis. op nama)") }, singleLine = true)
            Button(onClick = { ServerManager.sendCommand(cmd); cmd = "" }) { Text("Kirim") }
        }
    }
}
