package com.mcphone.app

/**
 * Link download. Aplikasi mengunduh file ini saat pertama kali dipakai.
 * - JRE_URL: JRE/JDK 21 untuk Android arm64, format .tar.gz atau .tar.xz. WAJIB diisi (lihat README).
 * - PAPER_VERSION: versi Minecraft Java untuk PaperMC (link jar dicari otomatis lewat API PaperMC).
 * - BEDROCK_JAR_URL: PowerNukkitX (server Bedrock berbasis Java).
 * - PLAYIT_URL: agent tunnel playit.gg untuk Linux aarch64.
 */
object Config {
    const val JRE_URL = ""
    const val PAPER_VERSION = "1.21.10"
    const val BEDROCK_JAR_URL = "https://github.com/PowerNukkitX/PowerNukkitX/releases/download/snapshot/powernukkitx.jar"
    const val PLAYIT_URL = "https://github.com/playit-cloud/playit-agent/releases/latest/download/playit-linux-aarch64"
}

enum class ServerType(val label: String, val port: Int) {
    JAVA("Java Edition", 25565),
    BEDROCK("Bedrock (HP/Konsol/Windows)", 19132)
}
