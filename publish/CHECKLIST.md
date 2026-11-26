# Langkah rilis ke Google Play

1. Daftar akun Google Play Console (biaya sekali bayar USD 25, plus verifikasi identitas).
2. Package name sekarang `id.thistime.alarm`. Ganti ke domain/nama milikmu SEKARANG kalau mau (cari-ganti di seluruh project, ganti nama folder `java/id/thistime/alarm`). Setelah diunggah ke Play, package name tidak bisa diubah.
3. Android Studio: Build > Generate Signed App Bundle / APK > Android App Bundle. Buat keystore baru, SIMPAN file dan passwordnya di tempat aman (hilang = tidak bisa update app). Hasilnya file .aab.
4. Play Console > Create app. Isi listing dari STORE_LISTING.md. Unggah store-icon-512.png dan feature-graphic-1024x500.png, plus minimal 2 screenshot dari HP.
5. Taruh privacy-policy.html di web publik (mudah: GitHub Pages), isi email kamu, lalu tempel URL-nya di App content.
6. Isi semua bagian App content (iklan, akses app, target usia, data safety, deklarasi izin layar penuh dan layanan latar depan, rating konten).
7. Uji dulu: unggah .aab ke Internal testing, pasang di HP lain, tes alarm di beberapa kondisi.
8. Akun personal baru: jalankan Closed testing dengan minimal 12 tester selama 14 hari berturut-turut, lalu ajukan akses produksi.
9. Setelah disetujui: Production > Create release > unggah .aab > kirim untuk review.
