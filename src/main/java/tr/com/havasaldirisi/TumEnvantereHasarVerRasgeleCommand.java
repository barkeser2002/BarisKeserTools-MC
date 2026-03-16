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

import java.util.Random;

public class TumEnvantereHasarVerRasgeleCommand implements CommandExecutor {

    private final Random random = new Random();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Bu komutu sadece oyuncular kullanabilir!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(Component.text("Bunun için yetkin yok!", NamedTextColor.RED));
            return true;
        }

        int affectedItems = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable damageable) {
                short maxDurability = item.getType().getMaxDurability();

                if (maxDurability > 0) {
                    int currentDamage = damageable.getDamage();
                    int remainingDurability = maxDurability - currentDamage;

                    if (remainingDurability > 0) {
                        int randomDamage = random.nextInt(remainingDurability) + 1;
                        damageable.setDamage(currentDamage + randomDamage);
                        item.setItemMeta(damageable);
                        affectedItems++;
                    }
                }
            }
        }

        player.sendMessage(Component.text("Envanterindeki " + affectedItems + " adet eşyaya rastgele hasar verildi!", NamedTextColor.GREEN));
        return true;
    }
}
