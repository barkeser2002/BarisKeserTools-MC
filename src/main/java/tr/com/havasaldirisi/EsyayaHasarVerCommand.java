package tr.com.havasaldirisi;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class EsyayaHasarVerCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Bu komutu sadece oyuncular kullanabilir!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "Bunun için yetkin yok!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Kullanım: /eşyayahasarver [sayı]");
            return true;
        }


        int durabilityHasar;
        try {
            durabilityHasar = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Geçersiz sayı: " + args[0]);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Elinde bir eşya tutmalısın!");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            int currentDamage = damageable.getDamage();
            damageable.setDamage(currentDamage + durabilityHasar);
            item.setItemMeta((ItemMeta) damageable);
            player.sendMessage(ChatColor.GREEN + "Eşyanın durability'sine " + durabilityHasar + " hasar verildi! (Yeni durability: " + damageable.getDamage() + ")");
        } else {
            player.sendMessage(ChatColor.RED + "Bu eşya durability'si olan bir eşya değil!");
        }

        return true;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("10", "50", "100", "500", "1000"); // Örnek hasar miktarları
        }
        return new java.util.ArrayList<>();
    }
}
