package tr.com.havasaldirisi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TntSilCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender.hasPermission("bariskesertools.admin") || sender.isOp()) {
            int count = 0;
            
            if (args.length > 0) {
                // Belirli dünyadan sil
                World targetWorld = Bukkit.getWorld(args[0]);
                if (targetWorld == null) {
                    sender.sendMessage(Component.text("'" + args[0] + "' adında dünya bulunamadı!", NamedTextColor.RED));
                    return true;
                }
                for (Entity entity : targetWorld.getEntitiesByClass(TNTPrimed.class)) {
                    entity.remove();
                    count++;
                }
                sender.sendMessage(Component.text("✔ " + targetWorld.getName() + " dünyasındaki " + count + " adet TNT silindi!", NamedTextColor.GREEN));
            } else {
                // Tüm dünyalardan sil
                for (World world : sender.getServer().getWorlds()) {
                    for (Entity entity : world.getEntitiesByClass(TNTPrimed.class)) {
                        entity.remove();
                        count++;
                    }
                }
                sender.sendMessage(Component.text("✔ Tüm dünyalardaki " + count + " adet patlamaya hazır TNT silindi!", NamedTextColor.GREEN));
            }
            
            // Ses efekti
            if (sender instanceof Player player) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
            }
        } else {
            sender.sendMessage(Component.text("Bunun için yetkin yok!", NamedTextColor.RED));
        }
        return true;
    }
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> worlds = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) {
                if (w.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    worlds.add(w.getName());
                }
            }
            return worlds;
        }
        return new ArrayList<>();
    }
}
