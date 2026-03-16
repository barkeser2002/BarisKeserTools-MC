package tr.com.havasaldirisi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("Bunun için yetkin yok!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Kullanım: ", NamedTextColor.YELLOW).append(Component.text("/bariskesertools <updatecheck|update|komutlar>", NamedTextColor.AQUA)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "updatecheck":
                sender.sendMessage(Component.text("Github üzerinden güncellemeler kontrol ediliyor...", NamedTextColor.GREEN));
                updater.checkForUpdates(sender, false);
                break;
            case "update":
                sender.sendMessage(Component.text("Güncelleme mevcutsa indirme işlemi başlatılıyor...", NamedTextColor.GREEN));
                updater.checkForUpdates(sender, true);
                break;
            case "komutlar":
                sender.sendMessage(Component.text("--- BarisKeserTools Komutları ---", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("/bariskesertools ", NamedTextColor.AQUA).append(Component.text("- Ana menü (updatecheck / update / komutlar)", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("/havasaldirisi ", NamedTextColor.AQUA).append(Component.text("- Hava saldırısı başlatan olta", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("/chunk-yiyici ", NamedTextColor.AQUA).append(Component.text("- Dibe kadar delen matkap TNT oltası", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("/tntsil ", NamedTextColor.AQUA).append(Component.text("- Dünyadaki tüm TNT'leri imha eder", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("/itemsil ", NamedTextColor.AQUA).append(Component.text("- Yerdeki tüm eşyaları/dropları siler", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("/eşyayahasarver ", NamedTextColor.AQUA).append(Component.text("- Elindeki eşyanın canını düşürür", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("/tum-envantere-hasar-ver-rasgele ", NamedTextColor.AQUA).append(Component.text("- Rastgele bütün bir zırh/alet setine hasar vurur", NamedTextColor.WHITE)));
                break;
            default:
                sender.sendMessage(Component.text("Bilinmeyen parametre! Geçerli parametreler: updatecheck, update, komutlar", NamedTextColor.RED));
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
