<p align="center">
  <img src="https://raw.githubusercontent.com/barkeser2002/BarisKeserTools-MC/main/bariskesertools.png" alt="BarisKeserTools Logo" width="300">
</p>

# BarisKeserTools-MC

**BarisKeserTools**, sunucunuz için birbirinden farklı ve yıkıcı eğlenceler sunarken aynı zamanda yönetimi ve sunucu performansını rahatlatacak kullanışlı komutları bir araya getiren bir yetkili/admin eklentisidir. **Paper, Spigot ve Purpur** gibi forkları %100 destekler.

## 🔥 Ne işe yarar?
Bu eklenti aslında iki farklı felsefeyi birleştirir: Yıkım (Hava saldırıları) ve Düzen (Toplu temizlik). Kendi eklentisi içerisindeki otomatik güncelleyici sayesinde Github'dan daima en son sürümü çeker ve bir sonraki başlatmada sunucuya kurar.

### 📜 Ana Özellikler & Komutlar (Tümü `bariskesertools.admin` izni gerektirir)

*   **/havasaldirisi [tnt-sayısı] [yukseklik] [patlama-suresi] [şekil] [hasar]**
    Bu eklentinin temel çıkış noktasıdır. Size özel büyüye sahip bir olta verir. Oltayı herhangi bir yere savurduğunuzda; havadan yeryüzüne `yıldız`, `orbital`, `çizgi`, `yağmur` veya `yuvarlak` gibi desenler çizerek muazzam bir hava saldırısı başlatabilirsiniz.

*   **/chunk-yiyici [20-500 arası güç]**
    Yeryüzünden patlamaya başlayarak Katman Kayası'na (-64) kadar ardışık TNT matkabı ile düz bir matkap etkisi (Drill) yaratan özel bir olta fırlatmanızı sağlar. En dibe kadar koca bir chunk'ı süpürebilirsiniz. Güç ayarı patlama dairesini genişletir.

*   **/tntsil**
    Dünyada patlamayı bekleyen tüm TNT'leri imha eder. Özellikle devasa hava saldırılarından sonra yaşanabilecek büyük lag krizlerini saniyeler içinde önler.

*   **/itemsil**
    Yerde duran tüm düşmüş objeleri/eşyaları siler. Lag engellemeye yöneliktir.

*   **/eşyayahasarver [miktar]** & **/tum-envantere-hasar-ver-rasgele**
    Oyuncuların kendi eşya kırılganlıklarını (durability) test edebilmesi ya da özel troll amaçlar için tasarlanmıştır. Belirli bir el eşyasına veya envanterinizdeki tüm zırhlara rastgele hasardüşüşü uygulayabilirsiniz.

*   **/bariskesertools <updatecheck|update|komutlar>**
    Otomatik güncelleyiciyi çalıştırarak yeni sürümleri denetler veya eklenti komut listesini ekrana sunar.

### ⚙️ Uyumluluk
Geriye dönük uyumluluk felsefesiyle **Java 17** ile derlendiğinden;
`1.20, 1.20.1, 1.20.4, 1.20.6` ve `1.21, 1.21.1, 1.21.4` arası **bütün ana sürümlerde** tamamen sorunsuz çalışır!
