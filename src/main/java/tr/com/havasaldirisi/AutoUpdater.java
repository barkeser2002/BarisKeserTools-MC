package tr.com.havasaldirisi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;

    public AutoUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startPeriodicCheck() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            checkForUpdates(null, true);
        }, 200L, 36000L);
    }

    public void checkForUpdates(CommandSender sender, boolean forceDownload) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                URL apiURL = new URL("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest");
                connection = (HttpURLConnection) apiURL.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "BarisKeserTools-Updater");
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                int statusCode = connection.getResponseCode();
                if (statusCode == 200) {
                    JsonObject json;
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                        json = JsonParser.parseReader(reader).getAsJsonObject();
                    }
                    String latestTag = json.get("tag_name").getAsString();
                    String currentVersion = plugin.getPluginMeta().getVersion();

                    String cleanLatest = latestTag.replace("v", "");
                    String cleanCurrent = currentVersion.replace("v", "");

                    if (!cleanLatest.equals(cleanCurrent)) {
                        sendMessage(sender, Component.text("[BarisKeserTools] ", NamedTextColor.YELLOW).append(Component.text("Yeni bir güncelleme mevcut! Versiyon: ", NamedTextColor.GREEN)).append(Component.text(latestTag, NamedTextColor.WHITE)));

                        if (json.has("assets") && !json.getAsJsonArray("assets").isEmpty()) {
                            String downloadUrl = json.getAsJsonArray("assets").get(0)
                                    .getAsJsonObject().get("browser_download_url").getAsString();

                            if (forceDownload) {
                                downloadFile(downloadUrl, sender);
                            } else {
                                sendMessage(sender, Component.text("[BarisKeserTools] ", NamedTextColor.YELLOW).append(Component.text("Hemen indirmek için ", NamedTextColor.GREEN)).append(Component.text("/bariskesertools update", NamedTextColor.WHITE)).append(Component.text(" komutunu kullanin.", NamedTextColor.GREEN)));
                            }
                        }
                    } else {
                        sendMessage(sender, Component.text("[BarisKeserTools] ", NamedTextColor.YELLOW).append(Component.text("Mevcut sürüm günceldir! (" + latestTag + ")", NamedTextColor.GREEN)));
                    }
                } else if (statusCode == 404) {
                    sendMessage(sender, Component.text("Github üzerinde herhangi bir sürüm bulunamadı (Henüz release yok).", NamedTextColor.RED));
                } else {
                    sendMessage(sender, Component.text("Güncelleme kontrol API hatası. HTTP kodu: " + statusCode, NamedTextColor.RED));
                }
            } catch (java.net.SocketTimeoutException e) {
                sendMessage(sender, Component.text("Güncelleme kontrolü zaman aşımına uğradı. GitHub'a erişilemiyor.", NamedTextColor.RED));
            } catch (Exception e) {
                sendMessage(sender, Component.text("Güncelleme kontrolü sırasında hata oluştu: " + e.getMessage(), NamedTextColor.RED));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void downloadFile(String fileUrl, CommandSender sender) {
        if (isUpdateReady) {
            sendMessage(sender, Component.text("[BarisKeserTools] ", NamedTextColor.YELLOW).append(Component.text("Güncelleme zaten '.jar' halinde indi! Sunucu restart bekliyor.", NamedTextColor.GREEN)));
            return;
        }

        HttpURLConnection connection = null;
        try {
            sendMessage(sender, Component.text("[BarisKeserTools] ", NamedTextColor.YELLOW).append(Component.text("Github'dan yeni jar dosyasi sunucuya çekiliyor...", NamedTextColor.GRAY)));
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "BarisKeserTools-Updater");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);

            File updateFolder = new File(plugin.getDataFolder().getParentFile(), plugin.getServer().getUpdateFolder());
            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }

            File updateFile = new File(updateFolder, "BarisKeserTools-update.jar");

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(updateFile)) {
                byte[] buffer = new byte[4096];
                int count;
                long totalBytes = 0;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                    totalBytes += count;
                }
                sendMessage(sender, Component.text("[BarisKeserTools] ", NamedTextColor.YELLOW).append(Component.text("İndirme tamamlandı! (" + (totalBytes / 1024) + " KB)", NamedTextColor.GRAY)));
            }

            isUpdateReady = true;
            sendMessage(sender, Component.text("[BarisKeserTools] ", NamedTextColor.YELLOW).append(Component.text("✔ Başarılı! Yeni sürüm update klasörüne indirildi.", NamedTextColor.GREEN)));
            sendMessage(sender, Component.text("[BarisKeserTools] ", NamedTextColor.YELLOW).append(Component.text("Sunucu YENİDEN BAŞLATILDIĞINDA (Restart) otomatik olarak kurulacak.", NamedTextColor.GREEN)));

        } catch (java.net.SocketTimeoutException e) {
            sendMessage(sender, Component.text("Dosya indirme zaman aşımına uğradı.", NamedTextColor.RED));
        } catch (Exception e) {
            sendMessage(sender, Component.text("Güncelleme indirilirken bir hata ile karşılaşıldı: " + e.getMessage(), NamedTextColor.RED));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void sendMessage(CommandSender sender, Component message) {
        if (sender != null) {
            sender.sendMessage(message);
        } else {
            plugin.getLogger().info(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(message));
        }
    }
}
