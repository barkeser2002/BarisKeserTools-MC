package tr.com.havasaldirisi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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
    private boolean isUpdateReady = false;

    public AutoUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startPeriodicCheck() {
        // İlk kontrol 10sn (200 tick) sonra. Daha sonraki döngüler 30 dakikada bir (30 * 60 * 20 = 36000 tick)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            checkForUpdates(null, true);
        }, 200L, 36000L);
    }

    public void checkForUpdates(CommandSender sender, boolean forceDownload) {
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

                    String cleanLatest = latestTag.replace("v", "");
                    String cleanCurrent = currentVersion.replace("v", "");

                    if (!cleanLatest.equals(cleanCurrent)) {
                        sendMessage(sender, "§e[BarisKeserTools] §aYeni bir güncelleme mevcut! Versiyon: §f" + latestTag);
                        
                        if (json.has("assets") && !json.getAsJsonArray("assets").isEmpty()) {
                            String downloadUrl = json.getAsJsonArray("assets").get(0)
                                    .getAsJsonObject().get("browser_download_url").getAsString();
                            
                            if (forceDownload) {
                                downloadFile(downloadUrl, sender);
                            } else {
                                sendMessage(sender, "§e[BarisKeserTools] §aHemen indirmek için §f/bariskesertools update §akomutunu kullanin.");
                            }
                        }
                    } else {
                        sendMessage(sender, "§e[BarisKeserTools] §aMevcut sürüm günceldir! (" + latestTag + ")");
                    }
                } else if (statusCode == 404) {
                    sendMessage(sender, "§cGithub üzerinde herhangi bir sürüm bulunamadı (Henüz release yok).");
                }
            } catch (Exception e) {
                sendMessage(sender, "§cGüncelleme kontrolü sırasında hata oluştu: " + e.getMessage());
            }
        });
    }

    private void downloadFile(String fileUrl, CommandSender sender) {
        if (isUpdateReady) {
            sendMessage(sender, "§e[BarisKeserTools] §aGüncelleme zaten '.jar' halinde indi! Sunucu restart bekliyor.");
            return;
        }

        try {
            sendMessage(sender, "§e[BarisKeserTools] §7Github'dan yeni jar dosyasi sunucuya çekiliyor...");
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "BarisKeserTools-Updater");

            File updateFolder = new File(plugin.getDataFolder().getParentFile(), plugin.getServer().getUpdateFolder());
            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }

            // Dosya adının BarisKeserTools_update.jar olmasını sağlıyoruz
            File updateFile = new File(updateFolder, "BarisKeserTools-update.jar");

            InputStream in = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(updateFile);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.close();
            in.close();

            isUpdateReady = true;
            sendMessage(sender, "§e[BarisKeserTools] §aBaşarılı! Yeni sürüm update klasörüne indirildi.");
            sendMessage(sender, "§e[BarisKeserTools] §aSunucu YENİDEN BAŞLATILDIĞINDA (Restart) otomatik olarak kurulacak.");
            
        } catch (Exception e) {
            sendMessage(sender, "§cGüncelleme indirilirken bir hata ile karşılaşıldı: " + e.getMessage());
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        // Eğer sender verilmişse (oyuncu vs.) chat'e yazar, verilmemişse konsola okutur.
        if (sender != null) {
            sender.sendMessage(message);
        } else {
            plugin.getLogger().info(message.replaceAll("§[0-9a-fA-Fk-oK-OrR]", ""));
        }
    }
}
