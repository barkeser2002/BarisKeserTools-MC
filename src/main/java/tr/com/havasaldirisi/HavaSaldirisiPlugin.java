package tr.com.havasaldirisi;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class HavaSaldirisiPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private NamespacedKey oltaKey;
    private NamespacedKey tntSayisiKey;
    private NamespacedKey yukseklikKey;
    private NamespacedKey yayilmaKey;
    private NamespacedKey patlamaSuresiKey;
    private NamespacedKey oyuncuHasariKey;

    @Override
    public void onEnable() {
        oltaKey = new NamespacedKey(this, "ozel_olta");
        tntSayisiKey = new NamespacedKey(this, "tnt_sayisi");
        yukseklikKey = new NamespacedKey(this, "yukseklik");
        yayilmaKey = new NamespacedKey(this, "yayilma");
        patlamaSuresiKey = new NamespacedKey(this, "patlama_suresi");
        oyuncuHasariKey = new NamespacedKey(this, "oyuncu_hasari");
        
        // Github Otomatik Güncellemeyi Başlat
        AutoUpdater updater = new AutoUpdater(this);
        updater.startPeriodicCheck();

        BarisKeserToolsCommand mainCommand = new BarisKeserToolsCommand(updater);
        if (getCommand("bariskesertools") != null) {
            getCommand("bariskesertools").setExecutor(mainCommand);
            getCommand("bariskesertools").setTabCompleter(mainCommand);
        }

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("havasaldirisi") != null) {
            getCommand("havasaldirisi").setExecutor(this);
            getCommand("havasaldirisi").setTabCompleter(this);
        }
        if (getCommand("tntsil") != null) {
            getCommand("tntsil").setExecutor(new TntSilCommand());
        }
        if (getCommand("itemsil") != null) {
            getCommand("itemsil").setExecutor(new ItemSilCommand());
        }
        if (getCommand("eşyayahasarver") != null) {
            getCommand("eşyayahasarver").setExecutor(new EsyayaHasarVerCommand());
        }
        if (getCommand("tum-envantere-hasar-ver-rasgele") != null) {
            getCommand("tum-envantere-hasar-ver-rasgele").setExecutor(new TumEnvantereHasarVerRasgeleCommand());
        }
        
        ChunkYiyiciCommand chunkYiyici = new ChunkYiyiciCommand(this);
        if (getCommand("chunk-yiyici") != null) {
            getCommand("chunk-yiyici").setExecutor(chunkYiyici);
            getCommand("chunk-yiyici").setTabCompleter(chunkYiyici);
        }
        getServer().getPluginManager().registerEvents(chunkYiyici, this);
        
        BuyukTntCommand buyukTntCommand = new BuyukTntCommand(this);
        if (getCommand("buyuktnt") != null) {
            getCommand("buyuktnt").setExecutor(buyukTntCommand);
            getCommand("buyuktnt").setTabCompleter(buyukTntCommand);
        }
        getServer().getPluginManager().registerEvents(buyukTntCommand, this);

        getLogger().info("Hava Saldirisi Eklentisi (Plugin) aktif edildi!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("havasaldirisi")) {
            if (sender instanceof Player player) {
                if (player.hasPermission("bariskesertools.admin") || player.isOp()) {
                    if (args.length < 5) {
                        player.sendMessage(ChatColor.RED + "Kullanım: /havasaldirisi [tntsayisi] [nekadarustte] [patlamasüresi_tick] [şekil] [oyuncuyahasar_miktari]");
                        return true;
                    }
                
                int tntSayisi;
                int yukseklik;
                int patlamaSuresi;
                double oyuncuHasari;
                
                try {
                    tntSayisi = Integer.parseInt(args[0]);
                    yukseklik = Integer.parseInt(args[1]);
                    patlamaSuresi = Integer.parseInt(args[2]);
                    oyuncuHasari = Double.parseDouble(args[4]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Lütfen geçerli sayılar (tnt, yükseklik, patlama süresi, hasar) girin!");
                    return true;
                }
                
                String yayilmaTipi = args[3].toLowerCase();
                List<String> validTypes = Arrays.asList("tekblokiçinde", "yuvarlak", "dağınıkrasgele", "kare", "yıldız", "çizgi", "yağmur", "orbital");
                if (!validTypes.contains(yayilmaTipi)) {
                    player.sendMessage(ChatColor.RED + "Geçersiz dizilim türü! Seçenekler: " + String.join(", ", validTypes));
                    return true;
                }
                
                if (tntSayisi > 2048) {
                    player.sendMessage(ChatColor.RED + "En fazla 2048 TNT ekleyebilirsin!");
                    tntSayisi = 2048;
                }
                
                ItemStack rod = new ItemStack(Material.FISHING_ROD);
                ItemMeta meta = rod.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Hava Saldırısı Oltası");
                    meta.getPersistentDataContainer().set(oltaKey, PersistentDataType.BYTE, (byte) 1);
                    meta.getPersistentDataContainer().set(tntSayisiKey, PersistentDataType.INTEGER, tntSayisi);
                    meta.getPersistentDataContainer().set(yukseklikKey, PersistentDataType.INTEGER, yukseklik);
                    meta.getPersistentDataContainer().set(patlamaSuresiKey, PersistentDataType.INTEGER, patlamaSuresi);
                    meta.getPersistentDataContainer().set(yayilmaKey, PersistentDataType.STRING, yayilmaTipi);
                    meta.getPersistentDataContainer().set(oyuncuHasariKey, PersistentDataType.DOUBLE, oyuncuHasari);
                    rod.setItemMeta(meta);
                }
                player.getInventory().addItem(rod);
                player.sendMessage(ChatColor.GREEN + "Hava Saldırısı Oltası (" + tntSayisi + " TNT, " + yukseklik + " Y, " + patlamaSuresi + " Tick, " + yayilmaTipi + ", Hasar: " + oyuncuHasari + ") başarıyla verildi!");
            } else {
                sender.sendMessage(ChatColor.RED + "Bunun için yetkin yok!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Bu komut sadece oyuncular içindir!");
        }
        return true;
    }
    
    return false;
}

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("havasaldirisi")) {
            if (args.length == 1) {
                return Arrays.asList("1", "5", "10", "50", "100");
            } else if (args.length == 2) {
                return Arrays.asList("10", "20", "30", "50");
            } else if (args.length == 3) {
                return Arrays.asList("20", "40", "60", "80", "100"); // 80 tick = 4s
            } else if (args.length == 4) {
                List<String> types = Arrays.asList("tekblokiçinde", "yuvarlak", "dağınıkrasgele", "kare", "yıldız", "çizgi", "yağmur", "orbital");
                List<String> completions = new ArrayList<>();
                for (String type : types) {
                    if (type.startsWith(args[3].toLowerCase())) {
                        completions.add(type);
                    }
                }
                return completions;
            } else if (args.length == 5) {
                return Arrays.asList("0.0", "5.0", "10.0", "20.0"); // örnek hasar değerleri (0 olursa kapatılmış sayılabilir vs.)
            }
        }
        return new ArrayList<>();
    }

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Olta kontrolü
        ItemStack usingItem = event.getItem();

        if (usingItem != null && usingItem.getType() == Material.FISHING_ROD && usingItem.getItemMeta() != null) {
            if (usingItem.getItemMeta().getPersistentDataContainer().has(oltaKey, PersistentDataType.BYTE)) {

                event.setCancelled(true); // Oltanın ip atmasını tamamen iptal et, sadece sağ tık mekaniği çalışsın!

                Location eyeLoc = player.getEyeLocation();
                
                int tntSayisi = usingItem.getItemMeta().getPersistentDataContainer().getOrDefault(tntSayisiKey, PersistentDataType.INTEGER, 1);
                int yukseklik = usingItem.getItemMeta().getPersistentDataContainer().getOrDefault(yukseklikKey, PersistentDataType.INTEGER, 25);
                int patlamaSuresi = usingItem.getItemMeta().getPersistentDataContainer().getOrDefault(patlamaSuresiKey, PersistentDataType.INTEGER, 80);
                String yayilmaTipi = usingItem.getItemMeta().getPersistentDataContainer().getOrDefault(yayilmaKey, PersistentDataType.STRING, "yuvarlak");
                
                if (tntSayisi > 2048) tntSayisi = 2048;

                // Mouse ile bakılan bloğun koordinatını bul
                Location targetLoc = null;
                int maxDistance = 100;
                org.bukkit.block.Block targetBlock = player.getTargetBlockExact(maxDistance);
                if (targetBlock != null && targetBlock.getType() != Material.AIR) {
                    targetLoc = targetBlock.getLocation().clone().add(0.5, yukseklik, 0.5);
                } else {
                    // Hiç blok yoksa, ileriye doğru git ve üstüne at
                    targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(20));
                    targetLoc.add(0, yukseklik, 0);
                }

                // 3) TNT'leri İstenilen Şekilde Spawnla
                double radius = Math.min(10.0, tntSayisi * 0.5); // Şekiller için genel yarıçap baz alınacak değer
                Random random = new Random();
                    
                    double finalHasar = usingItem.getItemMeta().getPersistentDataContainer().getOrDefault(oyuncuHasariKey, PersistentDataType.DOUBLE, -1.0);
                    List<TNTPrimed> doganTntler = new ArrayList<>();

                    if (tntSayisi == 1 || yayilmaTipi.equals("tekblokiçinde")) {
                        for (int i = 0; i < tntSayisi; i++) {
                            TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(targetLoc, TNTPrimed.class);
                            tnt.setFuseTicks(patlamaSuresi);
                            doganTntler.add(tnt);
                        }
                    } else if (yayilmaTipi.equals("dağınıkrasgele")) {
                        for (int i = 0; i < tntSayisi; i++) {
                            double xOffset = (random.nextDouble() * 2 * radius) - radius;
                            double zOffset = (random.nextDouble() * 2 * radius) - radius;
                            
                            Location spawnLoc = targetLoc.clone().add(xOffset, 0, zOffset);
                            TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(spawnLoc, TNTPrimed.class);
                            tnt.setFuseTicks(patlamaSuresi);
                            doganTntler.add(tnt);
                        }
                    } else if (yayilmaTipi.equals("kare")) {
                        // Basit kare dizilimi (ızgara)
                        int edge = (int) Math.ceil(Math.sqrt(tntSayisi));
                        double spacing = (radius * 2) / Math.max(1, edge - 1);
                        int count = 0;
                        for (int x = 0; x < edge; x++) {
                            for (int z = 0; z < edge; z++) {
                                if (count >= tntSayisi) break;
                                double xOffset = -radius + (x * spacing);
                                double zOffset = -radius + (z * spacing);
                                Location spawnLoc = targetLoc.clone().add(xOffset, 0, zOffset);
                                TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(spawnLoc, TNTPrimed.class);
                                tnt.setFuseTicks(patlamaSuresi);
                                doganTntler.add(tnt);
                                count++;
                            }
                        }
                    } else if (yayilmaTipi.equals("yıldız")) {
                        // 5 kollu veya n kollu basit yıldız şekli denemesi (çaprazlara uzanan çizgiler)
                        int arms = 5; 
                        int perArm = tntSayisi / arms;
                        int count = 0;
                        for (int arm = 0; arm < arms; arm++) {
                            double angle = 2 * Math.PI * arm / arms;
                            for (int i = 1; i <= perArm; i++) {
                                if (count >= tntSayisi) break;
                                double dist = (radius / perArm) * i;
                                double xOffset = dist * Math.cos(angle);
                                double zOffset = dist * Math.sin(angle);
                                Location spawnLoc = targetLoc.clone().add(xOffset, 0, zOffset);
                                TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(spawnLoc, TNTPrimed.class);
                                tnt.setFuseTicks(patlamaSuresi);
                                doganTntler.add(tnt);
                                count++;
                            }
                        }
                        // Geri kalanı merkeze
                        while (count < tntSayisi) {
                            TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(targetLoc, TNTPrimed.class);
                            tnt.setFuseTicks(patlamaSuresi);
                            doganTntler.add(tnt);
                            count++;
                        }
                    } else if (yayilmaTipi.equals("çizgi")) {
                        double spacing = (radius * 2) / Math.max(1, tntSayisi - 1);
                        for (int i = 0; i < tntSayisi; i++) {
                            double xOffset = -radius + (i * spacing);
                            Location spawnLoc = targetLoc.clone().add(xOffset, 0, 0); // X ekseninde düz çizgi
                            TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(spawnLoc, TNTPrimed.class);
                            tnt.setFuseTicks(patlamaSuresi);
                            doganTntler.add(tnt);
                        }
                    } else if (yayilmaTipi.equals("yağmur")) {
                        for (int i = 0; i < tntSayisi; i++) {
                            double yOffset = i * 2.0; // Her tnt arasına 2 blok dikey boşluk
                            Location spawnLoc = targetLoc.clone().add(0, yOffset, 0);
                            TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(spawnLoc, TNTPrimed.class);
                            tnt.setFuseTicks(patlamaSuresi);
                            doganTntler.add(tnt);
                        }
                    } else if (yayilmaTipi.equals("orbital")) {
                        // Yuvarlak içinde yuvarlak, ama yayılarak gidiyor velocity'si var
                        int rings = (int) Math.sqrt(tntSayisi) + 1;
                        int count = 0;
                        for (int r = 1; r <= rings; r++) {
                            int tntsInRing = r * 6;
                            double ringRadius = r * 1.5; // Giderek büyüyen halkalar
                            for (int i = 0; i < tntsInRing; i++) {
                                if (count >= tntSayisi) break;
                                double angle = 2 * Math.PI * i / tntsInRing;
                                double xOffset = ringRadius * Math.cos(angle);
                                double zOffset = ringRadius * Math.sin(angle);
                                
                                Location spawnLoc = targetLoc.clone().add(xOffset, 0, zOffset);
                                TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(spawnLoc, TNTPrimed.class);
                                tnt.setFuseTicks(patlamaSuresi);
                                
                                // Dışa doğru bir momentum (velocity) ekle
                                org.bukkit.util.Vector velocity = new org.bukkit.util.Vector(xOffset, 0, zOffset).normalize().multiply(0.5);
                                tnt.setVelocity(velocity);
                                doganTntler.add(tnt);
                                
                                count++;
                            }
                        }
                        // Geri kalanı merkeze
                        while (count < tntSayisi) {
                            TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(targetLoc, TNTPrimed.class);
                            tnt.setFuseTicks(patlamaSuresi);
                            doganTntler.add(tnt);
                            count++;
                        }
                    } else {
                        // "yuvarlak" (Varsayılan veya yuvarlak seçilmişse)
                        for (int i = 0; i < tntSayisi; i++) {
                            double angle = 2 * Math.PI * i / tntSayisi;
                            double xOffset = radius * Math.cos(angle);
                            double zOffset = radius * Math.sin(angle);
                            
                            Location spawnLoc = targetLoc.clone().add(xOffset, 0, zOffset);
                            TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(spawnLoc, TNTPrimed.class);
                            tnt.setFuseTicks(patlamaSuresi);
                            doganTntler.add(tnt);
                        }
                    }
                    
                    // TNT Hasarlarını Ayarla
                    if (finalHasar >= 0.0) {
                        for (TNTPrimed t : doganTntler) {
                            t.setMetadata("ozel_tnt_hasar", new FixedMetadataValue(this, finalHasar));
                        }
                    }

                    // 4) Oltanın dayanıklılığını (canını) 1 azalt
                    ItemMeta meta = usingItem.getItemMeta();
                    if (meta instanceof Damageable damageable) {
                        event.setCancelled(true); // Normal olta mekaniklerini iptal et
                        damageable.setDamage(damageable.getDamage() + 1);
                        usingItem.setItemMeta(damageable);

                        if (damageable.getDamage() >= usingItem.getType().getMaxDurability()) {
                            usingItem.setAmount(0);
                            // 8 blok yarıçapındaki tüm oyunculara sesi çal
                            double breakSoundRadius = 8.0;
                            Location breakLoc = player.getLocation();
                            for (Player p : player.getWorld().getPlayers()) {
                                if (p.getLocation().distanceSquared(breakLoc) <= breakSoundRadius * breakSoundRadius) {
                                    p.playSound(breakLoc, org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                                }
                            }
                        }
                    }
                }
            }
        }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof TNTPrimed tnt && event.getEntity() instanceof Player targetPlayer) {
            // Eğer tnt oyuncuya vuruyorsa hasarı değiştir
            if (tnt.hasMetadata("ozel_tnt_hasar")) {
                double ozelHasar = tnt.getMetadata("ozel_tnt_hasar").get(0).asDouble();
                if (ozelHasar == 0.0) {
                    event.setCancelled(true);
                } else {
                    event.setDamage(ozelHasar);
                }
            }
        }
    }
}
