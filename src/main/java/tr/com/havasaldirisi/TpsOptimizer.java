package tr.com.havasaldirisi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private final String GUI_NAME = ChatColor.DARK_RED + "TPS Optimizasyon Menüsü";

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
                sender.sendMessage("Bu komut sadece oyuncular icindir.");
                return true;
            }
            if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "Bunun icin yetkin yok!");
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

        inv.setItem(10, createGuiItem(Material.DIAMOND_PICKAXE, ChatColor.YELLOW + "Aninda Blok Toplama", blockDrops, "Bloklari kirildiginda direkt envantere alir,", "Yere dusen esya lagini engeller."));
        inv.setItem(12, createGuiItem(Material.ENDER_EYE, ChatColor.AQUA + "Oyuncu Render Optimizasyonu", playerRender, "Kalabalik alanlarda uzaktaki oyunculari", "NMS/Render motorundan gizler."));
        inv.setItem(14, createGuiItem(Material.TNT, ChatColor.RED + "Patlama Drop Optimizasyonu", explosionDrops, "TNT patlamalarindan cikan tum esyalari", "aninda silerek devasa FPS dususlerini onler."));
        inv.setItem(16, createGuiItem(Material.ZOMBIE_HEAD, ChatColor.DARK_PURPLE + "Yapay Zeka (Mob AI) Dondurma", mobAi, "Kalabalik chunklardaki canavarlarin", "yapay zekasini kapatarak islemciyi rahatlatir."));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name, boolean state, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String l : lore) loreList.add(ChatColor.GRAY + l);
            loreList.add("");
            loreList.add(state ? ChatColor.GREEN + " DURUM: ACIK (Tikla ve Kapat)" : ChatColor.RED + " DURUM: KAPALI (Tikla ve Ac)");
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        int slot = event.getSlot();
        String path = "";

        if (slot == 10) path = "tps_optimizer.block_drops";
        else if (slot == 12) path = "tps_optimizer.player_render";
        else if (slot == 14) path = "tps_optimizer.explosion_drops";
        else if (slot == 16) path = "tps_optimizer.mob_ai";
        else return;

        boolean currentState = plugin.getConfig().getBoolean(path);
        plugin.getConfig().set(path, !currentState);
        plugin.saveConfig();

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
        openGui(player); // Yenile
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
