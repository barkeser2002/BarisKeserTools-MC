package tr.com.havasaldirisi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChunkYiyiciCommand implements CommandExecutor, org.bukkit.command.TabCompleter, Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey oltaKey;
    private final NamespacedKey ilkGucKey;
    private final NamespacedKey matkapGucKey;
    private final NamespacedKey hizKey;
    
    // Cooldown: UUID → son kullanım zamanı (ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 2000; // 2 saniye

    public ChunkYiyiciCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.oltaKey = new NamespacedKey(plugin, "chunk_yiyici_olta");
        this.ilkGucKey = new NamespacedKey(plugin, "chunk_yiyici_ilk_guc");
        this.matkapGucKey = new NamespacedKey(plugin, "chunk_yiyici_matkap_guc");
        this.hizKey = new NamespacedKey(plugin, "chunk_yiyici_hiz");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Sadece oyuncular kullanabilir!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(Component.text("Bunun için yetkin yok!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 3) {
            player.sendMessage(Component.text("Kullanım: /chunk-yiyici [İlkTntGücü(1-30)] [MatkapGücü(1-100)] [Hız(1-5)]", NamedTextColor.RED));
            player.sendMessage(Component.text("Örnek (Normal TNT gibi başlangıç, devasa yıkım): /chunk-yiyici 4 100 5", NamedTextColor.YELLOW));
            return true;
        }

        int ilkGuc, matkapGuc, hiz;
        try {
            ilkGuc = Integer.parseInt(args[0]);
            matkapGuc = Integer.parseInt(args[1]);
            hiz = Integer.parseInt(args[2]);

            if (ilkGuc < 1 || ilkGuc > 30) {
                player.sendMessage(Component.text("İlk düşen TNT gücü en az 1, en fazla 30 olabilir! (Normal orijinal TNT = 4)", NamedTextColor.RED));
                return true;
            }
            if (matkapGuc < 1 || matkapGuc > 100) {
                player.sendMessage(Component.text("Yeri delen matkap gücü en az 1, en fazla 100 olabilir!", NamedTextColor.RED));
                return true;
            }
            if (hiz < 1 || hiz > 5) {
                player.sendMessage(Component.text("Hız seviyesi 1 ile 5 arasında olmalıdır! (En hızlı = 5)", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Lütfen sadece sayı kullanın!", NamedTextColor.RED));
            return true;
        }

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("☠ Chunk Yiyici Olta ☠", NamedTextColor.DARK_RED));
            meta.lore(Arrays.asList(
                    Component.empty(),
                    Component.text("İlk Patlama Gücü: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(ilkGuc), NamedTextColor.YELLOW)),
                    Component.text("Alt Matkap Gücü: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(matkapGuc), NamedTextColor.RED)),
                    Component.text("Yıkım Hızı Lvl: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(hiz), NamedTextColor.AQUA)),
                    Component.empty(),
                    Component.text("Sağ tık ile chunk'ı yok edin!", NamedTextColor.DARK_GRAY)
            ));
            
            // Enchant glow
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            meta.getPersistentDataContainer().set(oltaKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(ilkGucKey, PersistentDataType.INTEGER, ilkGuc);
            meta.getPersistentDataContainer().set(matkapGucKey, PersistentDataType.INTEGER, matkapGuc);
            meta.getPersistentDataContainer().set(hizKey, PersistentDataType.INTEGER, hiz);
            rod.setItemMeta(meta);
        }

        player.getInventory().addItem(rod);
        player.sendMessage(Component.text("☠ Chunk Yiyici Olta başarıyla verildi.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        return true;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("4", "10", "20", "30");
        } else if (args.length == 2) {
            return Arrays.asList("5", "10", "20", "50", "100");
        } else if (args.length == 3) {
            return Arrays.asList("1", "2", "3", "4", "5");
        }
        return new java.util.ArrayList<>();
    }

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getItemMeta().getPersistentDataContainer().has(oltaKey, PersistentDataType.BYTE)) {

            event.setCancelled(true);
            
            // Cooldown kontrolü
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && (now - lastUse) < COOLDOWN_MS) {
                long remaining = (COOLDOWN_MS - (now - lastUse)) / 1000 + 1;
                player.sendMessage(Component.text("⏳ Bu oltayı tekrar kullanmak için " + remaining + " saniye beklemelisiniz!", NamedTextColor.RED));
                return;
            }
            cooldowns.put(player.getUniqueId(), now);

            Block targetBlock = player.getTargetBlockExact(200);
            Location target;

            if (targetBlock != null) {
                target = targetBlock.getLocation();
            } else {
                Location eyeLoc = player.getEyeLocation();
                target = eyeLoc.clone().add(eyeLoc.getDirection().multiply(50));
            }

            ItemMeta meta = item.getItemMeta();
            int ilkGuc = meta.getPersistentDataContainer().getOrDefault(ilkGucKey, PersistentDataType.INTEGER, 4);
            int matkapGuc = meta.getPersistentDataContainer().getOrDefault(matkapGucKey, PersistentDataType.INTEGER, 50);
            int hiz = meta.getPersistentDataContainer().getOrDefault(hizKey, PersistentDataType.INTEGER, 3);

            Location spawnLoc = target.clone().add(0, 10, 0);
            TNTPrimed tnt = player.getWorld().spawn(spawnLoc, TNTPrimed.class);
            tnt.setYield((float) ilkGuc);
            tnt.setFuseTicks(200);

            tnt.setMetadata("chunk_yiyici_root", new FixedMetadataValue(plugin, true));
            tnt.setMetadata("chunk_yiyici_matkap_guc", new FixedMetadataValue(plugin, matkapGuc));
            tnt.setMetadata("chunk_yiyici_hiz", new FixedMetadataValue(plugin, hiz));

            // Ateşleme sesi
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f);

            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (tnt.isDead() || !tnt.isValid()) {
                        this.cancel();
                        return;
                    }
                    if (tnt.isOnGround() || tnt.getVelocity().lengthSquared() == 0.0) {
                        tnt.setFuseTicks(0);
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 4L, 1L);

            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                damageable.setDamage(damageable.getDamage() + 1);
                item.setItemMeta(damageable);

                if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                    item.setAmount(0);
                    double radius = 8.0;
                    Location breakLoc = player.getLocation();
                    for (Player p : player.getWorld().getPlayers()) {
                        if (p.getLocation().distanceSquared(breakLoc) <= radius * radius) {
                            p.playSound(breakLoc, org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed tnt) {
            if (tnt.hasMetadata("chunk_yiyici_root") || tnt.hasMetadata("chunk_yiyici_child")) {
                event.setYield(0f);
            }

            if (tnt.hasMetadata("chunk_yiyici_root")) {
                int matkapGuc = tnt.hasMetadata("chunk_yiyici_matkap_guc") ? tnt.getMetadata("chunk_yiyici_matkap_guc").get(0).asInt() : 50;
                int hiz = tnt.hasMetadata("chunk_yiyici_hiz") ? tnt.getMetadata("chunk_yiyici_hiz").get(0).asInt() : 3;

                Location loc = tnt.getLocation();
                World world = loc.getWorld();

                int currentY = loc.getBlockY();
                int minY = world.getMinHeight();

                int aralik = (matkapGuc <= 50) ? 5 : (matkapGuc <= 100) ? 10 : (matkapGuc <= 250) ? 15 : 20;
                int tickDelay = Math.max(1, (6 - hiz) * 2 - 1);
                int delay = tickDelay;

                for (int y = currentY - aralik; y > minY; y -= aralik) {
                    Location childLoc = new Location(world, loc.getX(), y, loc.getZ());
                    TNTPrimed childTnt = world.spawn(childLoc, TNTPrimed.class);
                    childTnt.setYield((float) matkapGuc);
                    childTnt.setFuseTicks(delay);
                    childTnt.setMetadata("chunk_yiyici_child", new FixedMetadataValue(plugin, true));
                    delay += tickDelay;
                }
            }
        }
    }
}
