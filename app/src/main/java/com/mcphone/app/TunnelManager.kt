package com.mcphone.app

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tunnel via agent playit.gg: membuat alamat publik supaya teman dari data seluler / WiFi lain / luar kota bisa masuk.
 * Mendukung TCP (Java) dan UDP (Bedrock). Pertama kali: buka link klaim sekali, lalu buat tunnel di dashboard playit.gg.
 */
object TunnelManager {
    val claimUrl = MutableStateFlow<String?>(null)
    val address = MutableStateFlow<String?>(null)
    val status = MutableStateFlow("Tunnel mati")
    private var process: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val claimRegex = Regex("https://playit\\.gg/claim/\\S+")
    private val addrRegex = Regex("[A-Za-z0-9.-]+\\.(?:ply\\.gg|joinmc\\.link)(?::\\d+)?")

    fun start(ctx: Context) {
        if (process != null) return
        scope.launch {
            try {
                val dir = File(ctx.filesDir, "playit").apply { mkdirs() }
                val bin = File(dir, "playit")
                if (!bin.exists()) {
                    require(Config.PLAYIT_URL.isNotBlank()) { "PLAYIT_URL belum diisi di Config.kt" }
                    status.value = "Mengunduh agent tunnel..."
                    val c = URL(Config.PLAYIT_URL).openConnection() as HttpURLConnection
                    c.inputStream.use { i -> bin.outputStream().use { o -> i.copyTo(o) } }
                    bin.setExecutable(true, false)
                }
                status.value = "Menyambungkan tunnel..."
                val pb = ProcessBuilder(bin.absolutePath).directory(dir).redirectErrorStream(true)
                pb.environment()["HOME"] = dir.absolutePath
                pb.environment()["XDG_CONFIG_HOME"] = dir.absolutePath // agar sesi klaim tersimpan
                process = pb.start()
                process!!.inputStream.bufferedReader().forEachLine { raw ->
                    val line = raw.replace(Regex("\u001B\\[[;\\d]*m"), "")
                    claimRegex.find(line)?.let { claimUrl.value = it.value; status.value = "Buka link klaim untuk menautkan akun" }
                    addrRegex.find(line)?.let { address.value = it.value; claimUrl.value = null; status.value = "Tunnel AKTIF" }
                }
            } catch (e: Exception) { status.value = "Tunnel gagal: ${e.message}" }
            finally { process = null }
        }
    }

    fun stop() {
        process?.destroy(); process = null
        claimUrl.value = null; address.value = null; status.value = "Tunnel mati"
    }
}
