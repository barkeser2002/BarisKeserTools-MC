package tr.com.havasaldirisi;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.jetbrains.annotations.NotNull;

public class ItemSilCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender.hasPermission("bariskesertools.admin") || sender.isOp()) {
            int count = 0;
            for (World world : sender.getServer().getWorlds()) {
                for (Entity entity : world.getEntitiesByClass(Item.class)) {
                    entity.remove();
                    count++;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Yerdeki " + count + " adet eşya başarıyla silindi!");
        } else {
            sender.sendMessage(ChatColor.RED + "Bunun için yetkin yok!");
        }
        return true;
    }
}
