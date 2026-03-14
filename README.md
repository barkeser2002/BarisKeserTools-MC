<p align="center">
  <img src="bariskesertools.png" alt="BarisKeserTools Logo" width="300">
</p>

# BarisKeserTools-MC 🛠️💥

BarisKeserTools, Minecraft (Paper/Spigot) sunucuları için gelişmiş araçlar, hasar yönetimi, oyuncu etkileşimleri ve yıkıcı "Hava Saldırısı" mekanikleri sunan modüler bir yetkili eklentisidir! Tüm Minecraft **1.20.X** ve **1.21.X** sürümleriyle eksiksiz ve geriye dönük uyumludur.

## 🌟 Özellikler

- **Gelişmiş Hava Saldırısı Oltası (Orbital Strike):** Havadan çeşitli desen ve şekillerde TNT yağdırmanıza yarayan özel fırlatıcı. Sekiz farklı yayılma paterni destekler!
- **Otomatik Tab Tamamlama:** Tüm komutlarda akıllı tab tamamlama (TabCompleter) ile kullanım kolaylığı.
- **Toplu Temizleme Araçları:** Sunucuyu lag'dan kurtarmak için bekleyen devasa TNT kalıntılarını veya yere düşmüş eşyaları silen güçlü optimizasyon komutları.
- **Hasar Testi Araçları:** Oyuncuların envanterlerindeki eşyaların rastgele veya belirli oranda kırılmasını sağlayan (durability) test ve şaka komutları.
- **Otomatik Güncelleme Sistemi:** Github üzerinden gelen updateleri otomatik algılayıp yeni çıkan sürümleri Update klasörüne çeker!

---

## 👨‍💻 Komutlar & Kullanımlar

Tüm komutlar için gereken ana yetki (Permission): `bariskesertools.admin`

### 1- 🧨 Hava Saldırısı (/havasaldirisi)
Özel güçlendirilmiş bir olta ile belirli hedeflere havadan topçu atağı (TNT) yönlendirir. Oltayı sağ-tık ile savurduğunuz yere bağlı olarak devasa etki bırakır. 
**Kullanım:** `/havasaldirisi [tntsayisi] [nekadarustte] [patlamasuresi_tick] [şekil] [oyuncuyahasar_miktari]`

**Desteklenen Şekiller (Paternler):**
- `tekblokiçinde`: Tüm TNT'ler tek sınırda doğar.
- `yuvarlak`: TNT'ler hedefin tepesinden devasa bir çember şeklinde yayılır.
- `dağınıkrasgele`: TNT'ler tamamen rastgele yerlere saçılır.
- `kare`: Düz bir ızgara-kare formatında inerler.
- `çizgi`: Art arda sıralanmış çizgi halinde düşerler.
- `yıldız`: Patlamadan önce yıldız düzeninde yayılırlar.
- `yağmur`: Kademeli ve sürekli düşen klasik bombardıman tarzı.
- `orbital`: İç içe geçen çemberler halinde dışarı doğru (velocity) itilirler!

*Not: "oyuncuyahasar_miktari" parametresi ile TNT'lerin patlama esnasında size veya diğer oyunculara vereceği tam matematiksel zararı belirtebilirsiniz.*

### 2- 🗑️ TNT Temizleme (/tntsil)
Dünyadaki (ve o an aktif olan) tüm patlamaya hazır (Primed) TNT'leri sunucudan silerek olası lag & çökmeleri anında durdurur.

### 3- 🧹 Eşya Temizleme (/itemsil)
Sunucuda yere düşmüş olan ve bekleyen bütün eşyaları siler. Lag engellemek için mükemmeldir.

### 4- 🗡️ Eşyaya Hasar Ver (/eşyayahasarver)
Oyuncunun sadece elinde tuttuğu hasar alabilen (durability sahibi) kazma, kılıç, zırh vb. ekipmana yazılan sayı kadar anında can azaltması uygular.
**Kullanım:** `/eşyayahasarver [hasar_miktarı]` *(Örn; `/eşyayahasarver 100`)*

### 5- 🎲 Tüm Envantere Rastgele Hasar (/tum-envantere-hasar-ver-rasgele)
Hedef oyuncunun envanterinde yer alan tüm eşyalara teker teker göz atar ve her birinin maksimum can miktarından yola çıkarak "kalan canına göre" rastgele oranda hasar vurur.

---

## ⚙️ Teknik Gereksinimler

- Sunucu Sürümü: Yapısal olarak `.jar` **Java 17** ile **1.20** hedeflenerek derlenmiştir ve **1.21.x** mekaniklerini de destekler.
- API: Paper (Tavsiye edilir), Spigot veya Purpur vb. forklar.
- Geliştirici: Barış Keser

## 🔄 Otomatik Yayın (CI/CD) Modülü
Bu depo (`barkeser2002/BarisKeserTools-MC`), bir **GitHub Actions** ile donatılmıştır. Ne zaman kod değişse ve *build.gradle.kts* üzerindeki versiyon ilerlerse, sunucu sürümü otomatik build edip **Release/latest** olarak yayınlar. Oyuna gömülü Updater sistemi de sunucu açılırken anında bu Release dosyasını görebilir.
