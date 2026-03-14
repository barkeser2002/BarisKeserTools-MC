package tr.com.havasaldirisi;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BarisKeserToolsCommand implements CommandExecutor, TabCompleter {

    private final AutoUpdater updater;

    public BarisKeserToolsCommand(AutoUpdater updater) {
        this.updater = updater;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("bariskesertools.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Bunun için yetkin yok!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Kullanım: " + ChatColor.AQUA + "/bariskesertools <updatecheck|update|komutlar>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "updatecheck":
                sender.sendMessage(ChatColor.GREEN + "Github üzerinden güncellemeler kontrol ediliyor...");
                updater.checkForUpdates(sender, false);
                break;
            case "update":
                sender.sendMessage(ChatColor.GREEN + "Güncelleme mevcutsa indirme işlemi başlatılıyor...");
                updater.checkForUpdates(sender, true);
                break;
            case "komutlar":
                sender.sendMessage(ChatColor.GOLD + "--- BarisKeserTools Komutları ---");
                sender.sendMessage(ChatColor.AQUA + "/bariskesertools " + ChatColor.WHITE + "- Ana menü (updatecheck / update / komutlar)");
                sender.sendMessage(ChatColor.AQUA + "/havasaldirisi " + ChatColor.WHITE + "- Hava saldırısı başlatan olta");
                sender.sendMessage(ChatColor.AQUA + "/chunk-yiyici " + ChatColor.WHITE + "- Dibe kadar delen matkap TNT oltası");
                sender.sendMessage(ChatColor.AQUA + "/tntsil " + ChatColor.WHITE + "- Dünyadaki tüm TNT'leri imha eder");
                sender.sendMessage(ChatColor.AQUA + "/itemsil " + ChatColor.WHITE + "- Yerdeki tüm eşyaları/dropları siler");
                sender.sendMessage(ChatColor.AQUA + "/eşyayahasarver " + ChatColor.WHITE + "- Elindeki eşyanın canını düşürür");
                sender.sendMessage(ChatColor.AQUA + "/tum-envantere-hasar-ver-rasgele " + ChatColor.WHITE + "- Rastgele bütün bir zırh/alet setine hasar vurur");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Bilinmeyen parametre! Geçerli parametreler: updatecheck, update, komutlar");
                break;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("bariskesertools.admin")) {
            return Arrays.asList("updatecheck", "update", "komutlar").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
