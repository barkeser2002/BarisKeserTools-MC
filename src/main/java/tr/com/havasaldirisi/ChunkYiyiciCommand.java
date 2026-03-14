package tr.com.havasaldirisi;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ChunkYiyiciCommand implements CommandExecutor, org.bukkit.command.TabCompleter, Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey oltaKey;
    private final NamespacedKey ilkGucKey;
    private final NamespacedKey matkapGucKey;
    private final NamespacedKey hizKey;

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
            sender.sendMessage(ChatColor.RED + "Sadece oyuncular kullanabilir!");
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "Bunun için yetkin yok!");
            return true;
        }

        if (args.length != 3) {
            player.sendMessage(ChatColor.RED + "Kullanım: /chunk-yiyici [İlkTntGücü(1-30)] [MatkapGücü(1-100)] [Hız(1-5)]");
            player.sendMessage(ChatColor.YELLOW + "Örnek (Normal TNT gibi başlangıç, devasa yıkım): /chunk-yiyici 4 100 5");
            return true; // İlk TNT normal oyunda 4 gücündedir.
        }

        int ilkGuc, matkapGuc, hiz;
        try {
            ilkGuc = Integer.parseInt(args[0]);
            matkapGuc = Integer.parseInt(args[1]);
            hiz = Integer.parseInt(args[2]);

            if (ilkGuc < 1 || ilkGuc > 30) {
                player.sendMessage(ChatColor.RED + "İlk düşen TNT gücü en az 1, en fazla 30 olabilir! (Normal orijinal TNT = 4)");
                return true;
            }
            if (matkapGuc < 1 || matkapGuc > 100) {
                player.sendMessage(ChatColor.RED + "Yeri delen matkap gücü en az 1, en fazla 100 olabilir!");
                return true;
            }
            if (hiz < 1 || hiz > 5) {
                player.sendMessage(ChatColor.RED + "Hız seviyesi 1 ile 5 arasında olmalıdır! (En hızlı = 5)");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Lütfen sadece sayı kullanın!");
            return true;
        }

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "☠ Chunk Yiyici Olta ☠");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "İlk Patlama Gücü: " + ChatColor.YELLOW + ilkGuc,
                    ChatColor.GRAY + "Alt Matkap Gücü: " + ChatColor.RED + matkapGuc,
                    ChatColor.GRAY + "Yıkım Hızı Lvl: " + ChatColor.AQUA + hiz
            ));
            // Verileri oltanın içine işliyoruz ki çekildiğinde bunlardan yararlanabilelim
            meta.getPersistentDataContainer().set(oltaKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(ilkGucKey, PersistentDataType.INTEGER, ilkGuc);
            meta.getPersistentDataContainer().set(matkapGucKey, PersistentDataType.INTEGER, matkapGuc);
            meta.getPersistentDataContainer().set(hizKey, PersistentDataType.INTEGER, hiz);
            rod.setItemMeta(meta);
        }

        player.getInventory().addItem(rod);
        player.sendMessage(ChatColor.GREEN + "Chunk Yiyici Olta başarıyla verildi.");
        return true;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("4", "10", "20", "30"); // İlk patlama gücü önerileri
        } else if (args.length == 2) {
            return Arrays.asList("5", "10", "20", "50", "100"); // Matkap gücü önerileri
        } else if (args.length == 3) {
            return Arrays.asList("1", "2", "3", "4", "5"); // Hız seviyesi önerileri
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
            
            event.setCancelled(true); // Oltanın ip atmasını (animasyonu) tamamen durdur

            // --- İSTENEN DEĞİŞİKLİK: Artık iğnenin düştüğü yer değil "Baktığımız yer" hedefleniyor ---
            Block targetBlock = player.getTargetBlockExact(200); // 200 blok uzağa kadar oyuncunun gözünün çarptığı bloğu alır
            Location target;
            
            if (targetBlock != null) {
                target = targetBlock.getLocation(); // Bloğa bakıyorsa
            } else {
                Location eyeLoc = player.getEyeLocation();
                target = eyeLoc.clone().add(eyeLoc.getDirection().multiply(50)); // Eğer tamamen havaya bakıyorsa 50 blok ileri atar
            }

            // Oltanın metadatasından kaydedilmiş sayıları okuma
            ItemMeta meta = item.getItemMeta();
            int ilkGuc = meta.getPersistentDataContainer().getOrDefault(ilkGucKey, PersistentDataType.INTEGER, 4);
            int matkapGuc = meta.getPersistentDataContainer().getOrDefault(matkapGucKey, PersistentDataType.INTEGER, 50);
            int hiz = meta.getPersistentDataContainer().getOrDefault(hizKey, PersistentDataType.INTEGER, 3);

            Location spawnLoc = target.clone().add(0, 10, 0); // Hedefin tam 10 blok yukarısı
            TNTPrimed tnt = player.getWorld().spawn(spawnLoc, TNTPrimed.class);
            tnt.setYield((float) ilkGuc); // Oltaya atanan "İlk TNT" gücü devreye girdi.
            tnt.setFuseTicks(200); // Havada kalma süresi uzun tutulur (Yere değene kadar Max 10 sn)
            
            // Alt matkaba yayılacak olan gücü ve hızı bu ilk TNT'ye etiket olarak yapıştırıyoruz
            tnt.setMetadata("chunk_yiyici_root", new FixedMetadataValue(plugin, true));
            tnt.setMetadata("chunk_yiyici_matkap_guc", new FixedMetadataValue(plugin, matkapGuc));
            tnt.setMetadata("chunk_yiyici_hiz", new FixedMetadataValue(plugin, hiz));

            // TNT'yi takip edip yere değdiğinde anında patlatacak Scheduler
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (tnt.isDead() || !tnt.isValid()) {
                        this.cancel();
                        return;
                    }
                    
                    // Eğer TNT yere değmişse (isOnGround) veya block içindeyse anında parçalanır.
                    if (tnt.isOnGround() || tnt.getVelocity().lengthSquared() == 0.0) {
                        tnt.setFuseTicks(0); // Süreyi sıfırla, hemen patla!
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 4L, 1L); // 4 tick (0.2sn) sonra kontrol etmeye başla, sonra her tick (0.05sn) devam et.
            
            // Oltanın dayanıklılığını (canını) 1 azalt
            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                damageable.setDamage(damageable.getDamage() + 1);
                item.setItemMeta(damageable);

                if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                    item.setAmount(0);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        }
    }

    @EventHandler

    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed tnt) {

            // Yıkım lagını önlemek için blok kırılmalarının eşya düşürmesini kapatır. Güç yüksek olursa sunucu donar.
            if (tnt.hasMetadata("chunk_yiyici_root") || tnt.hasMetadata("chunk_yiyici_child")) {
                event.setYield(0f);
            }

            // Aşağıya kadar delinecek alanın komutları (İlk TNT yere değip patladığında tetiklenir)
            if (tnt.hasMetadata("chunk_yiyici_root")) {
                int matkapGuc = tnt.hasMetadata("chunk_yiyici_matkap_guc") ? tnt.getMetadata("chunk_yiyici_matkap_guc").get(0).asInt() : 50;
                int hiz = tnt.hasMetadata("chunk_yiyici_hiz") ? tnt.getMetadata("chunk_yiyici_hiz").get(0).asInt() : 3;

                Location loc = tnt.getLocation();
                World world = loc.getWorld();
                
                int currentY = loc.getBlockY();
                int minY = world.getMinHeight(); // Dünyanın alt sınırını otomatik tanır (-64 gibi)
                
                // Güce göre TNT mesafesi dinamiği. (Dibe ne kadar sık aralıkla ineceği)
                int aralik = (matkapGuc <= 50) ? 5 : (matkapGuc <= 100) ? 10 : (matkapGuc <= 250) ? 15 : 20;

                // --- İSTENEN DEĞİŞİKLİK: 1-5 Hız Seviyesi --- 
                // Lvl 1 = 9 tick (0.45sn), Lvl 2 = 7 tick, Lvl 3 = 5 tick, Lvl 4 = 3 tick, Lvl 5 = 1 tick (0.05sn - Anında deler!)
                int tickDelay = Math.max(1, (6 - hiz) * 2 - 1); 
                int delay = tickDelay; 
                
                // Patlayan TNT'den başlayarak alt tabakaya kadar bir hat oluşturulur
                for (int y = currentY - aralik; y > minY; y -= aralik) {
                    Location childLoc = new Location(world, loc.getX(), y, loc.getZ());
                    TNTPrimed childTnt = world.spawn(childLoc, TNTPrimed.class);
                    childTnt.setYield((float) matkapGuc); // Oltaya yazılan "Matkap gücü" burada aktiftir
                    childTnt.setFuseTicks(delay); // Sırayla patlamaları için ayarlanan tick uygulanır
                    childTnt.setMetadata("chunk_yiyici_child", new FixedMetadataValue(plugin, true));
                    
                    delay += tickDelay; // Bir sonrakine ekstra gecikme ekler
                }
            }
        }
    }
}
