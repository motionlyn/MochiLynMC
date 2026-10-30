# MCPhone
Aplikasi Android untuk menjalankan server Minecraft langsung dari HP.

## Cara build lewat GitHub
1. Buat repo baru di GitHub, upload semua file proyek ini (branch `main`).
2. Edit `app/src/main/java/com/mcphone/app/Config.kt`: link server Java/Bedrock dan tunnel sudah terisi. Yang WAJIB kamu isi: `JRE_URL` (JRE 21 untuk Android arm64, .tar.gz/.tar.xz).
3. Tab **Actions** → workflow **Build APK** jalan otomatis (atau Run workflow).
4. Selesai → unduh artifact `MCPhone-debug-apk`, ekstrak, install APK di HP.

## Catatan
- `targetSdk = 28` sengaja, agar Android mengizinkan menjalankan Java dari data aplikasi (tidak bisa masuk Play Store, aman untuk sideload).
- Pemain di jaringan WiFi yang sama: `IP-HP:port` (Java 25565, Bedrock 19132).
- Tunnel (pemain dari luar WiFi/data seluler): `PLAYIT_URL` sudah terisi (agent playit.gg linux-aarch64).
  Pertama kali: tekan "Tautkan akun playit.gg", login, lalu di dashboard playit.gg buat tunnel (Minecraft Java = TCP, Bedrock = UDP).
  Alamat publik akan muncul di aplikasi, bagikan ke teman.
