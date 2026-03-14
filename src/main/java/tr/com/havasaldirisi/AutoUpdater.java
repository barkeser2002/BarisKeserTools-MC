package tr.com.havasaldirisi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AutoUpdater {

    private final JavaPlugin plugin;
    private final String repoOwner = "barkeser2002";
    private final String repoName = "BarisKeserTools-MC";

    public AutoUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL apiURL = new URL("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "BarisKeserTools-Updater");

                int statusCode = connection.getResponseCode();
                if (statusCode == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    String latestTag = json.get("tag_name").getAsString();
                    String currentVersion = plugin.getDescription().getVersion();

                    // V sembolünü vs. temizliyoruz ki version numaraları sadece rakam olsun (örn v1.1 -> 1.1)
                    String cleanLatest = latestTag.replace("v", "");
                    String cleanCurrent = currentVersion.replace("v", "");

                    if (!cleanLatest.equals(cleanCurrent)) {
                        plugin.getLogger().warning("========================================");
                        plugin.getLogger().warning("Yeni bir eklenti surumu bulundu! Versiyon: " + latestTag);
                        plugin.getLogger().warning("Mevcut kullandıgınız surum: " + currentVersion);
                        plugin.getLogger().warning("Guncel JAR GitHub uzerinden indiriliyor...");
                        plugin.getLogger().warning("========================================");

                        if (json.has("assets") && !json.getAsJsonArray("assets").isEmpty()) {
                            String downloadUrl = json.getAsJsonArray("assets").get(0)
                                    .getAsJsonObject().get("browser_download_url").getAsString();
                            downloadFile(downloadUrl);
                        } else {
                            plugin.getLogger().warning("Surum bulunmasina ragmen release assets icinde .jar dosyasi bulunamadi!");
                        }
                    } else {
                        plugin.getLogger().info("Plugin Github uzerindeki guncel en son surumu (" + latestTag + ") kullaniyor.");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Github uzerinden guncelleme kontrolu sirasinda bir hata ile karsilasildi: " + e.getMessage());
            }
        });
    }

    private void downloadFile(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "BarisKeserTools-Updater");

            // Spigot / Paper Update klasörü mantığı - update klasörüne inen dosyalar bir dahaki restart'da eski dosyanın yerine geçer
            File updateFolder = new File(plugin.getDataFolder().getParentFile(), plugin.getServer().getUpdateFolder());
            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }

            File updateFile = new File(updateFolder, "BarisKeserTools-" + plugin.getDescription().getVersion() + "-update.jar");

            InputStream in = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(updateFile);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.close();
            in.close();

            plugin.getLogger().info("Yeni eklenti dosyasi basariyla " + updateFolder.getName() + " klasorune indirildi!");
            plugin.getLogger().info("Sunucu YENIDEN BASLATILDIGINDA (Restart) veya /reload atildiginda guncelleme otomatik uygulanacaktir.");
        } catch (Exception e) {
            plugin.getLogger().warning("Guncelleme dosyasi indirilirken bir hata olustu: " + e.getMessage());
        }
    }
}
