package tr.com.havasaldirisi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TpsOptimizer implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final Component GUI_NAME = Component.text("⚙ TPS Optimizasyon Menüsü", NamedTextColor.DARK_RED);

    public TpsOptimizer(JavaPlugin plugin) {
        this.plugin = plugin;

        plugin.getConfig().addDefault("tps_optimizer.block_drops", true);
        plugin.getConfig().addDefault("tps_optimizer.player_render", true);
        plugin.getConfig().addDefault("tps_optimizer.explosion_drops", true);
        plugin.getConfig().addDefault("tps_optimizer.mob_ai", true);
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        startPlayerRenderOptimizer();
        startMobAiOptimizer();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("baris-optimizasyon")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Bu komut sadece oyuncular icindir."));
                return true;
            }
            if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
                player.sendMessage(Component.text("Bunun icin yetkin yok!", NamedTextColor.RED));
                return true;
            }
            openGui(player);
            return true;
        }
        return false;
    }

    private void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        boolean blockDrops = plugin.getConfig().getBoolean("tps_optimizer.block_drops");
        boolean playerRender = plugin.getConfig().getBoolean("tps_optimizer.player_render");
        boolean explosionDrops = plugin.getConfig().getBoolean("tps_optimizer.explosion_drops");
        boolean mobAi = plugin.getConfig().getBoolean("tps_optimizer.mob_ai");

        // Boş slotları siyah cam panelle doldur
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.text(" "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler.clone());
        }

        inv.setItem(10, createGuiItem(Material.DIAMOND_PICKAXE, Component.text("⛏ Anında Blok Toplama", NamedTextColor.YELLOW), blockDrops, "Blokları kırıldığında direkt envantere alır,", "Yere düşen eşya lagını engeller."));
        inv.setItem(12, createGuiItem(Material.ENDER_EYE, Component.text("👁 Oyuncu Render Optimizasyonu", NamedTextColor.AQUA), playerRender, "Kalabalık alanlarda uzaktaki oyuncuları", "NMS/Render motorundan gizler."));
        inv.setItem(14, createGuiItem(Material.TNT, Component.text("💣 Patlama Drop Optimizasyonu", NamedTextColor.RED), explosionDrops, "TNT patlamalarından çıkan tüm eşyaları", "anında silerek devasa FPS düşüşlerini önler."));
        inv.setItem(16, createGuiItem(Material.ZOMBIE_HEAD, Component.text("🧟 Yapay Zeka (Mob AI) Dondurma", NamedTextColor.DARK_PURPLE), mobAi, "Kalabalık chunklardaki canavarların", "yapay zekasını kapatarak işlemciyi rahatlatır."));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, Component name, boolean state, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            List<Component> loreList = new ArrayList<>();
            for (String l : lore) loreList.add(Component.text(l, NamedTextColor.GRAY));
            loreList.add(Component.empty());
            loreList.add(state
                ? Component.text("✔ DURUM: AÇIK", NamedTextColor.GREEN).append(Component.text(" (Tıkla ve Kapat)", NamedTextColor.DARK_GREEN))
                : Component.text("✘ DURUM: KAPALI", NamedTextColor.RED).append(Component.text(" (Tıkla ve Aç)", NamedTextColor.DARK_RED)));
            meta.lore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component title = event.getView().title();
        if (!title.equals(GUI_NAME)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        int slot = event.getSlot();
        String path = "";
        String featureName = "";

        if (slot == 10) { path = "tps_optimizer.block_drops"; featureName = "Anında Blok Toplama"; }
        else if (slot == 12) { path = "tps_optimizer.player_render"; featureName = "Oyuncu Render Optimizasyonu"; }
        else if (slot == 14) { path = "tps_optimizer.explosion_drops"; featureName = "Patlama Drop Optimizasyonu"; }
        else if (slot == 16) { path = "tps_optimizer.mob_ai"; featureName = "Mob AI Dondurma"; }
        else return;

        boolean currentState = plugin.getConfig().getBoolean(path);
        boolean newState = !currentState;
        plugin.getConfig().set(path, newState);
        plugin.saveConfig();

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
        
        // Durum değişikliğinde bilgi mesajı
        if (newState) {
            player.sendMessage(Component.text("✔ ", NamedTextColor.GREEN)
                .append(Component.text(featureName, NamedTextColor.YELLOW))
                .append(Component.text(" aktif edildi!", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("✘ ", NamedTextColor.RED)
                .append(Component.text(featureName, NamedTextColor.YELLOW))
                .append(Component.text(" devre dışı bırakıldı.", NamedTextColor.RED)));
        }
        
        openGui(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("tps_optimizer.block_drops")) return;

        Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) return;

        Block block = event.getBlock();
        Collection<ItemStack> drops = block.getDrops(p.getInventory().getItemInMainHand());

        if (!drops.isEmpty()) {
            event.setDropItems(false);
            for (ItemStack drop : drops) {
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(drop);
                if (!leftover.isEmpty()) {
                    for (ItemStack left : leftover.values()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), left);
                    }
                }
            }
        }

        int exp = event.getExpToDrop();
        if (exp > 0) {
            event.setExpToDrop(0);
            p.giveExp(exp);
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, (float)(1.5 + Math.random() * 0.5));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("tps_optimizer.explosion_drops")) return;
        event.setYield(0.0f);
    }

    private void startPlayerRenderOptimizer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("tps_optimizer.player_render")) return;

                for (Player p1 : Bukkit.getOnlinePlayers()) {
                    if (p1.getWorld().getPlayers().size() < 25) {
                        for (Player p2 : p1.getWorld().getPlayers()) {
                            if (p1 != p2 && !p1.canSee(p2)) {
                                p1.showPlayer(plugin, p2);
                            }
                        }
                        continue;
                    }

                    for (Player p2 : p1.getWorld().getPlayers()) {
                        if (p1 == p2) continue;

                        double distSq = p1.getLocation().distanceSquared(p2.getLocation());
                        if (distSq > (55.0 * 55.0)) {
                            if (p1.canSee(p2)) {
                                p1.hidePlayer(plugin, p2);
                            }
                        } else if (distSq < (45.0 * 45.0)) {
                            if (!p1.canSee(p2)) {
                                p1.showPlayer(plugin, p2);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 40L);
    }

    private void startMobAiOptimizer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("tps_optimizer.mob_ai")) return;

                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                        int entityCount = 0;
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                                entityCount++;
                            }
                        }

                        boolean overload = entityCount > 15;
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                                if (living.hasAI() == overload) {
                                    living.setAI(!overload);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 60L);
    }
}
