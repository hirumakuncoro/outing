# Outing

Aplikasi Android lokal untuk pengingat jadwal outing. Data jadwal diimpor dari JSON saat aplikasi berjalan. Tidak memakai server atau izin internet.

## Prasyarat

- GitHub repository private bernama `outing-reminder`
- JDK 17 bila build lokal
- Android phone API 29+

## Build lewat GitHub Actions

1. Buat repository private di GitHub, lalu tambahkan remote.
2. Push branch `main`.
3. Buka tab **Actions** → workflow **Build APK**.
4. Setelah selesai, unduh artefak `outing-debug`.
5. Ekstrak ZIP, kirim `app-debug.apk` ke HP, lalu instal.

APK debug sudah ditandatangani otomatis.

## Build lokal

Gradle wrapper belum disertakan. Jika Gradle 8.7 dan Android SDK tersedia:

```bash
gradle testDebugUnitTest --no-daemon
gradle assembleDebug --no-daemon
```

APK hasil build: `app/build/outputs/apk/debug/app-debug.apk`.

## Data jadwal

- Mulai dari `data/schedule.example.json`.
- Simpan jadwal asli sebagai `data/schedule.json`; file ini sengaja di-ignore Git.
- Kirim file JSON ke HP, kemudian impor dari tab Setup aplikasi.
- Semua waktu memakai epoch millis UTC dan ditampilkan sebagai WIB (`Asia/Jakarta`).

Jangan commit jadwal asli atau keystore.
