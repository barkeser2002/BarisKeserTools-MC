package tr.com.havasaldirisi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
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
            meta.displayName(Component.text("💣 Devasa Büyük TNT 💣", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true));
            meta.lore(Arrays.asList(
                Component.empty(),
                Component.text("Güç: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(guc), NamedTextColor.YELLOW)),
                Component.empty(),
                Component.text("Bu TNT'yi yere koyup çakmakla", NamedTextColor.GRAY),
                Component.text("yaktığınızda gökyüzünden dev bir TNT düşer!", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("⚠ Dikkat: Çok güçlü patlama!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, true)
            ));
            meta.getPersistentDataContainer().set(gucKey, PersistentDataType.INTEGER, guc);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Bunu sadece oyuncular kullanabilir.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(Component.text("Bunun için yetkin yok!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Kullanım: /buyuktnt [Güç(1-2000)]", NamedTextColor.RED));
            return true;
        }

        int guc;
        try {
            guc = Integer.parseInt(args[0]);
            if (guc < 1 || guc > 2000) {
                player.sendMessage(Component.text("Güç değeri 1 ile 2000 arasında olmalıdır!", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Lütfen geçerli bir tam sayı girin.", NamedTextColor.RED));
            return true;
        }

        player.getInventory().addItem(createTntItem(guc));
        player.sendMessage(Component.text("💣 Devasa TNT (Güç: " + guc + ") envanterinize eklendi! Yere koyup yakın.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        return true;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.TNT && item.hasItemMeta()) {
            if (item.getItemMeta().getPersistentDataContainer().has(gucKey, PersistentDataType.INTEGER)) {
                int guc = item.getItemMeta().getPersistentDataContainer().getOrDefault(gucKey, PersistentDataType.INTEGER, 100);
                Location blockLoc = event.getBlock().getLocation();
                placedTnts.put(blockLoc, guc);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (placedTnts.containsKey(loc)) {
            int guc = placedTnts.remove(loc);
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(loc, createTntItem(guc));
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onTntPrime(TNTPrimeEvent event) {
        Location loc = event.getBlock().getLocation();
        if (placedTnts.containsKey(loc)) {
            int guc = placedTnts.remove(loc);
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            triggerFallingTnt(loc, guc);
        }
    }

    private void triggerFallingTnt(Location origin, int guc) {
        float scale = 10.0f;
        Location spawnLoc = origin.clone().add(0.5, 90.0, 0.5);
        TNTPrimed tnt = origin.getWorld().spawn(spawnLoc, TNTPrimed.class);
        tnt.setYield((float) guc);
        tnt.setFuseTicks(400);

        String version = Bukkit.getServer().getBukkitVersion();
        boolean isModern = version.contains("1.21") || version.contains("1.20.5") || version.contains("1.20.6");

        if (isModern) {
            String cmd = "attribute " + tnt.getUniqueId() + " minecraft:generic.scale base set " + String.format(java.util.Locale.US, "%.2f", scale);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        
        // Ateşlenme duyurusu
        for (Player p : origin.getWorld().getPlayers()) {
            p.sendMessage(Component.text("⚠ ", NamedTextColor.RED)
                .append(Component.text("DEVASA TNT ATEŞLENDİ!", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" (Güç: " + guc + ")", NamedTextColor.YELLOW)));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tnt.isDead() || !tnt.isValid()) {
                    this.cancel();
                    return;
                }
                if (tnt.isOnGround() || tnt.getVelocity().lengthSquared() == 0.0) {
                    tnt.setFuseTicks(20);
                    this.cancel();
                } else {
                    // Düşerken partikül efekti
                    tnt.getWorld().spawnParticle(Particle.FLAME, tnt.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
                    tnt.getWorld().spawnParticle(Particle.SMOKE_NORMAL, tnt.getLocation(), 3, 0.3, 0.3, 0.3, 0.01);
                    
                    if (tnt.getFuseTicks() % 10 == 0) {
                        tnt.getWorld().playSound(tnt.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2f, 1.8f);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 1L);
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