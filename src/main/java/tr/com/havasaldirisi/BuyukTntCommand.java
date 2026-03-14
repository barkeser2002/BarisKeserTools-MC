package tr.com.havasaldirisi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuyukTntCommand implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey gucKey;
    private final Map<Location, Integer> placedTnts = new HashMap<>();

    public BuyukTntCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gucKey = new NamespacedKey(plugin, "buyuktnt_guc");
    }

    private ItemStack createTntItem(int guc) {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "Devasa Büyük TNT");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Güç: " + ChatColor.YELLOW + guc,
                ChatColor.GRAY + "Bu TNT'yi yere koyup çakmakla",
                ChatColor.GRAY + "yaktığınızda gökyüzünden dev bir TNT düşer!"
            ));
            meta.getPersistentDataContainer().set(gucKey, PersistentDataType.INTEGER, guc);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Bunu sadece oyuncular kullanabilir.");
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "Bunun için yetkin yok!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Kullanım: /buyuktnt [Güç(100-2000)]");
            return true;
        }

        int guc;
        try {
            guc = Integer.parseInt(args[0]);
            if (guc < 100 || guc > 2000) {
                player.sendMessage(ChatColor.RED + "Güç değeri 100 ile 2000 arasında olmalıdır!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Lütfen geçerli bir tam sayı girin.");
            return true;
        }

        player.getInventory().addItem(createTntItem(guc));
        player.sendMessage(ChatColor.GREEN + "Özel Devasa TNT eşyası envanterinize eklendi! Yere koyup yakın.");

        return true;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.TNT && item.hasItemMeta()) {
            if (item.getItemMeta().getPersistentDataContainer().has(gucKey, PersistentDataType.INTEGER)) {
                int guc = item.getItemMeta().getPersistentDataContainer().getOrDefault(gucKey, PersistentDataType.INTEGER, 100);
                placedTnts.put(event.getBlock().getLocation(), guc);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (placedTnts.containsKey(loc)) {
            int guc = placedTnts.remove(loc);
            event.setDropItems(false); // Normal TNT düşmesini engelle
            event.getBlock().getWorld().dropItemNaturally(loc, createTntItem(guc)); // Özel eşya olarak düşsün
        }
    }

    @EventHandler
    public void onTntPrime(TNTPrimeEvent event) {
        Location loc = event.getBlock().getLocation();
        if (placedTnts.containsKey(loc)) {
            int guc = placedTnts.remove(loc);
            
            // Normal ateşlemeyi iptal edip bloğu havaya uçuruyoruz (siliniyor)
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            
            triggerFallingTnt(loc, guc);
        }
    }

    private void triggerFallingTnt(Location origin, int guc) {
        // Değerlerin orantılı büyümesini sağlayan yegane ölçek formülü
        // 100 güç -> 1.0 boyut, 2000 güç -> 20.0 boyut
        float scale = 1.0f + ((float) (guc - 100) / 1900f) * 19.0f;

        Location spawnLoc = origin.clone().add(0.5, 90.0, 0.5); // Yerden 90 blok yukarıda belirir
        TNTPrimed tnt = origin.getWorld().spawn(spawnLoc, TNTPrimed.class);
        tnt.setYield((float) guc); // TNT Patlama Gücü
        tnt.setFuseTicks(400); // Havada süzülmesi için geniş süre ver (yere değince aniden patlayacağı için)
        
        String version = Bukkit.getServer().getBukkitVersion();
        boolean isModern = version.contains("1.21") || version.contains("1.20.5") || version.contains("1.20.6");

        if (isModern) {
            // Türkçe ayarlarda ',' yerine '.' kullanarak tam geçerli bir komut yapısı (Görsel büyüme)
            String cmd = "attribute " + tnt.getUniqueId() + " minecraft:generic.scale base set " + String.format(java.util.Locale.US, "%.2f", scale);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tnt.isDead() || !tnt.isValid()) {
                    this.cancel();
                    return;
                }

                // Yere düştüğünde veya tamamen hareketsiz kaldığında
                if (tnt.isOnGround() || tnt.getVelocity().lengthSquared() == 0.0) {
                    tnt.setFuseTicks(20); // 1 saniye (20 tick) sonra patla!
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 10L, 1L); // Yarım saniye bekleyip her tick kontrol etmeye başla
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("100", "500", "1000", "1500", "2000");
        }
        return new ArrayList<>();
    }
}