package tr.com.havasaldirisi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Bu komutu sadece oyuncular kullanabilir!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(Component.text("Bunun için yetkin yok!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Kullanım: /eşyayahasarver [sayı]", NamedTextColor.RED));
            return true;
        }

        int durabilityHasar;
        try {
            durabilityHasar = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Geçersiz sayı: " + args[0], NamedTextColor.RED));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(Component.text("Elinde bir eşya tutmalısın!", NamedTextColor.RED));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int currentDamage = damageable.getDamage();
            int maxDurability = item.getType().getMaxDurability();
            damageable.setDamage(currentDamage + durabilityHasar);
            item.setItemMeta(damageable);
            
            int remainingDurability = maxDurability - damageable.getDamage();
            player.sendMessage(Component.text("✔ Eşyanın durability'sine ", NamedTextColor.GREEN)
                .append(Component.text(durabilityHasar + " hasar", NamedTextColor.YELLOW))
                .append(Component.text(" verildi!", NamedTextColor.GREEN)));
            player.sendMessage(Component.text("   Kalan: ", NamedTextColor.GRAY)
                .append(Component.text(remainingDurability + "/" + maxDurability, 
                    remainingDurability > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)));
        } else {
            player.sendMessage(Component.text("Bu eşya durability'si olan bir eşya değil!", NamedTextColor.RED));
        }

        return true;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("10", "50", "100", "500", "1000");
        }
        return new java.util.ArrayList<>();
    }
}
