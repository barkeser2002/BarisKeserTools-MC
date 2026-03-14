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

import java.util.Random;

public class TumEnvantereHasarVerRasgeleCommand implements CommandExecutor {

    private final Random random = new Random();

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

        int affectedItems = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable) {
                short maxDurability = item.getType().getMaxDurability();
                
                if (maxDurability > 0) {
                    Damageable damageable = (Damageable) meta;
                    int currentDamage = damageable.getDamage();
                    int remainingDurability = maxDurability - currentDamage;

                    // Eğer eşya zaten kırılmamışsa
                    if (remainingDurability > 0) {
                        // Kalan dayanıklılık kadar rastgele bir hasar belirle (1 ile remainingDurability arası)
                        int randomDamage = random.nextInt(remainingDurability) + 1;
                        
                        damageable.setDamage(currentDamage + randomDamage);
                        item.setItemMeta((ItemMeta) damageable);
                        affectedItems++;
                    }
                }
            }
        }

        player.sendMessage(ChatColor.GREEN + "Envanterindeki " + affectedItems + " adet eşyaya rastgele hasar verildi!");
        return true;
    }
}
