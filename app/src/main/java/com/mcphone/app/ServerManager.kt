package com.mcphone.app

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL

object ServerManager {
    val status = MutableStateFlow("Belum siap")
    val running = MutableStateFlow(false)
    val log = MutableStateFlow<List<String>>(emptyList())
    private var process: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun addLog(line: String) { log.value = (log.value + line).takeLast(300) }

    fun localIp(): String = try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
            ?.hostAddress ?: "tidak terhubung WiFi"
    } catch (e: Exception) { "tidak diketahui" }

    private fun download(url: String, dest: File) {
        require(url.isNotBlank()) { "URL belum diisi di Config.kt" }
        dest.parentFile?.mkdirs()
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000
        c.inputStream.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
    }

    /** Cari link jar PaperMC build stabil terbaru lewat API resmi PaperMC. */
    private fun resolvePaper(): String {
        val c = URL("https://fill.papermc.io/v3/projects/paper/versions/${Config.PAPER_VERSION}/builds").openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "MCPhone/0.1 (aplikasi server Minecraft Android)")
        val arr = JSONArray(c.inputStream.bufferedReader().readText())
        var url: String? = null
        for (i in 0 until arr.length()) {
            val b = arr.getJSONObject(i)
            val u = b.getJSONObject("downloads").getJSONObject("server:default").getString("url")
            if (b.optString("channel") == "STABLE") { url = u; break }
            if (url == null) url = u
        }
        return url ?: error("Build PaperMC tidak ditemukan")
    }

    private fun extractTar(src: File, outDir: File) {
        val raw = src.inputStream().buffered()
        val stream = if (src.name.endsWith(".xz")) XZCompressorInputStream(raw) else GzipCompressorInputStream(raw)
        TarArchiveInputStream(stream).use { tar ->
            generateSequence { tar.nextTarEntry }.forEach { e ->
                val f = File(outDir, e.name)
                if (!f.canonicalPath.startsWith(outDir.canonicalPath)) return@forEach // anti path traversal
                if (e.isDirectory) f.mkdirs() else if (!e.isSymbolicLink) {
                    f.parentFile?.mkdirs()
                    f.outputStream().use { tar.copyTo(it) }
                    f.setExecutable(true, false)
                }
            }
        }
    }

    private fun findJava(ctx: Context): File? =
        File(ctx.filesDir, "jre").walkTopDown().firstOrNull { it.isFile && it.name == "java" && it.parentFile?.name == "bin" }

    fun start(ctx: Context, type: ServerType, ramMb: Int, onReady: () -> Unit = {}) {
        if (running.value) return
        running.value = true
        scope.launch {
            try {
                val jreDir = File(ctx.filesDir, "jre")
                if (findJava(ctx) == null) {
                    status.value = "Mengunduh Java (sekali saja)..."
                    val tmp = File(ctx.cacheDir, if (Config.JRE_URL.endsWith(".xz")) "jre.tar.xz" else "jre.tar.gz")
                    download(Config.JRE_URL, tmp); extractTar(tmp, jreDir); tmp.delete()
                }
                val java = findJava(ctx) ?: error("Java tidak ditemukan setelah ekstrak")
                val dir = File(ctx.filesDir, "server-${type.name.lowercase()}").apply { mkdirs() }
                val jar = File(dir, "server.jar")
                if (!jar.exists()) { status.value = "Mengunduh server..."; download(if (type == ServerType.JAVA) resolvePaper() else Config.BEDROCK_JAR_URL, jar) }
                if (type == ServerType.JAVA) File(dir, "eula.txt").writeText("eula=true\n")
                status.value = "Menjalankan server..."
                val cmd = mutableListOf(java.absolutePath, "-Xmx${ramMb}M")
                if (type == ServerType.BEDROCK) cmd += listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.io=ALL-UNNAMED")
                cmd += listOf("-jar", jar.absolutePath, "nogui")
                val pb = ProcessBuilder(cmd)
                    .directory(dir).redirectErrorStream(true)
                pb.environment()["HOME"] = dir.absolutePath
                process = pb.start()
                status.value = "Server AKTIF — ${localIp()}:${type.port}"
                onReady()
                process!!.inputStream.bufferedReader().forEachLine { addLog(it) }
                process!!.waitFor()
                status.value = "Server berhenti"
            } catch (e: Exception) {
                status.value = "Gagal: ${e.message}"; addLog("ERROR: ${e}")
            } finally { running.value = false; process = null }
        }
    }

    fun sendCommand(cmd: String) {
        try { process?.outputStream?.apply { write((cmd + "\n").toByteArray()); flush() } } catch (_: Exception) {}
    }

    fun stop() {
        TunnelManager.stop()
        sendCommand("stop")
        scope.launch { delay(10000); process?.destroy() }
    }
}
