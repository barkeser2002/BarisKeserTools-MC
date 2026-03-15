package tr.com.havasaldirisi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FloorIsLavaCommand implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, LavaGame> activeGames = new HashMap<>();
    private final Map<String, Integer> pendingBorders = new HashMap<>();
    
    // --- Etkinlik Toplanma Durumu ---
    public boolean isGathering = false;
    public final Set<UUID> participants = new HashSet<>();
    public BossBar gatheringBar = null;

    public FloorIsLavaCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Sadece oyuncular bu komutu kullanabilir.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("etkinligekatil")) {
            if (!isGathering) {
                player.sendMessage(ChatColor.RED + "Şu anda toplanan veya oyuncu bekleyen bir etkinlik yok!");
                return true;
            }
            if (participants.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "Etkinliğe zaten katıldınız, bekleyiniz!");
                return true;
            }
            participants.add(player.getUniqueId());
            if (gatheringBar != null) {
                gatheringBar.addPlayer(player);
                gatheringBar.setTitle(ChatColor.GOLD + "Etkinlik Bekleniyor - Katılımcı: " + ChatColor.GREEN + participants.size());
            }
            player.sendMessage(ChatColor.GREEN + "Etkinliğe başarıyla katıldınız! Lütfen oyun başlayana kadar bekleyiniz.");
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "Bunun için yetkiniz yok.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Kullanım: /lavaisfloor <kur|topla|baslat|iptal|sil> [parametreler]");
            player.sendMessage(ChatColor.GRAY + "→ /lavaisfloor topla (Sunucuda katılımı açar)");
            player.sendMessage(ChatColor.GRAY + "→ /lavaisfloor baslat [border] [lav_suresi_dk] [pvp_suresi_dk]");
            return true;
        }

        World world = player.getWorld();

        if (args[0].equalsIgnoreCase("kur")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Kullanım: /lavaisfloor kur <dünya_adi> [maxborder]");
                return true;
            }
            String worldName = args[1];
            
            if (args.length > 2) {
                try {
                    int border = Integer.parseInt(args[2]);
                    pendingBorders.put(worldName, border);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Border boyutu geçersiz, varsayılan (200) kullanılacak.");
                }
            }

            player.sendMessage(ChatColor.GREEN + "Multiverse-Core ile '" + worldName + "' dünyası oluşturuluyor. Okyanus olmayan bir bölge aranacak...");
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mv create " + worldName + " normal");
            
            new BukkitRunnable() {
                int attempts = 0;
                @Override
                public void run() {
                    World w = Bukkit.getWorld(worldName);
                    if (w != null) {
                        Location loc = w.getSpawnLocation();
                        int biomeAttempts = 0;
                        
                        int borderSizeForPreload = pendingBorders.getOrDefault(worldName, 200);
                        int checkRadius = borderSizeForPreload / 2;
                        
                        // Oyun alanı(Sınır) boyunca Okyanus/Nehir vb. barındırmayan devasa bir kara parçası bulana kadar ara
                        boolean badArea = true;
                        while (biomeAttempts < 300 && badArea) {
                            badArea = false;
                            
                            // Merkezden başlayarak etraftaki alanlardan örnek biyomlar kontrol ediliyor
                            // Sadece bir noktayı değil chunk chunk alanı tarıyor
                            int step = Math.max(16, checkRadius / 4);
                            
                            checkLoop:
                            for (int dx = -checkRadius; dx <= checkRadius; dx += step) {
                                for (int dz = -checkRadius; dz <= checkRadius; dz += step) {
                                    // Y: 64 verip geçiyoruz ki getHighestY ile yüzlerce chunk yaratıp lag yapmasın
                                    String bName = w.getBiome(loc.getBlockX() + dx, 64, loc.getBlockZ() + dz).name();
                                    if (bName.contains("OCEAN") || bName.contains("RIVER") || bName.contains("SWAMP") || bName.contains("BEACH") || bName.contains("WATER")) {
                                        badArea = true;
                                        break checkLoop; // Kötü biyom bulundu, direkt diğer alana atla
                                    }
                                }
                            }
                            
                            if (badArea) {
                                loc.add(1000, 0, 1000); // 1000 blok öteye koca bir adaya atıl
                                biomeAttempts++;
                            }
                        }
                        
                        loc.setY(w.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ()) + 1);
                        w.setSpawnLocation(loc); // Yeni susuz spawn noktası
                        
                        player.sendMessage(ChatColor.GREEN + "► Dünya yaratıldı ve belirlenen oyun sınırı (" + borderSizeForPreload + ") bölgesi denizden ARINDIRILMIŞ kıtaya taşındı!");
                        player.sendMessage(ChatColor.GREEN + "► Yeni Oyun Merkezi: (X:" + loc.getBlockX() + " Z:" + loc.getBlockZ() + ")");
                        
                        // Chunky preload
                        int chunkyRadius = checkRadius + 150; // Border'ın bir tık fazlası (Yarıçap + 150 blok)
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "chunky world " + worldName);
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "chunky center " + loc.getBlockX() + " " + loc.getBlockZ());
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "chunky radius " + chunkyRadius);
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "chunky start");
                        player.sendMessage(ChatColor.AQUA + "► Chunky ile ön yükleme (Preload) işlemi o bölge için başlatıldı! (Yarıçap: " + chunkyRadius + ")");
                        
                        player.sendMessage(ChatColor.YELLOW + "► Oraya gitmek için: /mv tp " + worldName);
                        this.cancel();
                    }
                    
                    if (attempts++ > 100) { // 100 saniye bekledi hala yoksa pes et
                        player.sendMessage(ChatColor.RED + "Dünya yaratılma süresi çok uzadı, otomatik biyom düzeltmesi pas geçildi.");
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 40L, 20L); // 2 sn sonra başla, saniyede bir dünyayı kontrol et
            
            return true;

        } else if (args[0].equalsIgnoreCase("topla")) {
            isGathering = true;
            participants.clear();
            if (gatheringBar != null) {
                gatheringBar.removeAll();
            }
            gatheringBar = Bukkit.createBossBar(ChatColor.GOLD + "Etkinlik Bekleniyor - Katılımcı: " + ChatColor.GREEN + "0", BarColor.GREEN, BarStyle.SOLID);
            
            Bukkit.broadcastMessage(ChatColor.GREEN + "=================================================");
            Bukkit.broadcastMessage(ChatColor.GOLD + "   YENİ BİR FLOOR IS LAVA ETKİNLİĞİ BAŞLIYOR!   ");
            Bukkit.broadcastMessage(ChatColor.YELLOW + " 1. Oyuna katılmak ve ışınlanmak için komut: " + ChatColor.AQUA + "/etkinligekatil");
            Bukkit.broadcastMessage(ChatColor.YELLOW + " 2. Envanteriniz boş ve hazır olunuz!");
            Bukkit.broadcastMessage(ChatColor.GREEN + "=================================================");
            
            return true;

        } else if (args[0].equalsIgnoreCase("baslat")) {
            if (activeGames.containsKey(world.getUID())) {
                player.sendMessage(ChatColor.RED + "Bulunduğunuz dünyada zaten bir oyun çalışıyor veya hazırlık aşamasında!");
                return true;
            }
            if (participants.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Hiç kimse katılmadığı için oyunu başlatamazsın! Önce /lavaisfloor topla ile insanları çağır!");
                return true;
            }

            // Kurulurken girilen bordersize'ı kontrol et
            int borderSize = pendingBorders.getOrDefault(world.getName(), 200);
            int lavSuresiDk = 20;
            int pvpSuresiDk = 10;
            
            // Eğer özellikle argüman verilmişse onu ezer
            if (args.length > 1) {
                try {
                    borderSize = Integer.parseInt(args[1]);
                    if (borderSize < 100 || borderSize > 5000) {
                        player.sendMessage(ChatColor.RED + "Border boyutu 100 ile 5000 arasında olmalıdır.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Lütfen geçerli bir sınır sayısı girin.");
                    return true;
                }
            }
            if (args.length > 2) {
                try {
                    lavSuresiDk = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Lütfen geçerli bir lav süresi (dakika) girin.");
                    return true;
                }
            }
            if (args.length > 3) {
                try {
                    pvpSuresiDk = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Lütfen geçerli bir pvp süresi (dakika) girin.");
                    return true;
                }
            }
            
            // Toplama aşamasını bitir
            isGathering = false;
            if (gatheringBar != null) {
                gatheringBar.removeAll();
            }

            activeGames.put(world.getUID(), null); // Hazırlık safhası başlasın
            startPreparationPhase(player, world, borderSize, lavSuresiDk, pvpSuresiDk);
            
            return true;

        } else if (args[0].equalsIgnoreCase("iptal")) {
            LavaGame game = activeGames.remove(world.getUID());
            if (game != null) {
                game.cleanup();
                player.sendMessage(ChatColor.GREEN + "Bu dünyadaki Zemin Lav oyunu başarıyla DURDURULDU.");
            } else {
                player.sendMessage(ChatColor.RED + "Bu dünyada devam eden aktif bir zemin lav oyunu bulunmuyor.");
            }
            return true;

        } else if (args[0].equalsIgnoreCase("sil")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Kullanım: /lavaisfloor sil <dünya_adi>");
                return true;
            }
            String worldName = args[1];
            World targetWorld = Bukkit.getWorld(worldName);
            
            if (targetWorld != null) {
                // Aktif oyun varsa kapat
                LavaGame game = activeGames.remove(targetWorld.getUID());
                if (game != null) game.cleanup();

                // İçindeki oyuncuları at
                World defaultWorld = Bukkit.getWorlds().get(0);
                for (Player p : targetWorld.getPlayers()) {
                    p.teleport(defaultWorld.getSpawnLocation());
                    p.sendMessage(ChatColor.RED + "Bulunduğunuz dünya silindiği için ana dünyaya gönderildiniz.");
                }
            }

            // Ardından MV ile sil
            player.sendMessage(ChatColor.RED + "'" + worldName + "' dünyası siliniyor...");
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + worldName);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mv confirm");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Bilinmeyen alt komut. Komutlar: kur, topla, baslat, iptal, sil");
        return true;
    }

    private void startPreparationPhase(Player admin, World world, int borderSize, int lavSuresiDk, int pvpSuresiDk) {
        List<Player> targetPlayers = new ArrayList<>();
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                targetPlayers.add(p);
            }
        }
        
        int totalPlayers = targetPlayers.size();
        if (totalPlayers == 0) totalPlayers = 1; // Hata almamak için minimum 1 yap
        
        Location center = admin.getLocation();
        double radius = Math.max(5.0, totalPlayers / 1.5); // Çember boyutu otomatik ayarlanır
        
        // Sınırları hazırlık aşamasındayken önceden ayarla (admine göre merkezle)
        world.getWorldBorder().setCenter(center);
        world.getWorldBorder().setSize(borderSize);
        world.getWorldBorder().setDamageBuffer(0);
        
        int i = 0;
        for (Player p : targetPlayers) {
            double angle = 2 * Math.PI * i / totalPlayers;
            double dx = radius * Math.cos(angle);
            double dz = radius * Math.sin(angle);
            
            Location tLoc = center.clone().add(dx, 0, dz);
            int highestY = world.getHighestBlockYAt(tLoc.getBlockX(), tLoc.getBlockZ());
            tLoc.setY(highestY + 1); // Blok üstüne ışınla
            
            // Oyuncunun yüzünü merkeze döndür
            tLoc.setDirection(center.toVector().subtract(tLoc.toVector()));
            
            p.teleport(tLoc);
            i++;
        }
        
        BossBar prepBar = Bukkit.createBossBar(ChatColor.YELLOW + "Oyun Hazırlanıyor...", BarColor.BLUE, BarStyle.SOLID);
        for (Player p : targetPlayers) {
            prepBar.addPlayer(p);
        }

        new BukkitRunnable() {
            int time = 60; // 1 Dakikalık hazırlık
            
            @Override
            public void run() {
                // Eğer döngü esnasında iptal komutu gelmişse temizle
                if (!activeGames.containsKey(world.getUID())) {
                    prepBar.removeAll();
                    this.cancel();
                    return;
                }

                if (time == 60) {
                    for (Player p : targetPlayers) {
                        p.sendMessage(ChatColor.AQUA + "=================================");
                        p.sendMessage(ChatColor.GOLD + "      FLOOR IS LAVA BAŞLIYOR!      ");
                        p.sendMessage(ChatColor.GRAY + "1. İlk " + pvpSuresiDk + " dakika maden ve gelişme süresidir (PvP Kapalı).");
                        p.sendMessage(ChatColor.GRAY + "2. Oyunun " + pvpSuresiDk + ". dakikasından itibaren PvP herkes için açılır!");
                        p.sendMessage(ChatColor.GRAY + "3. " + lavSuresiDk + ". dakikanın sonunda lavlar en dipten (-64) yükselmeye başlar.");
                        p.sendMessage(ChatColor.GRAY + "4. Her 30 saniyede bir lav tabakası 1 blok artar.");
                        p.sendMessage(ChatColor.AQUA + "=================================");
                    }
                } else if (time == 45) {
                    for (Player p : targetPlayers) p.sendMessage(ChatColor.YELLOW + "» Madene inip eşyalar bulmayı ve yükseklerde üs kurmayı unutmayın!");
                } else if (time == 30) {
                    for (Player p : targetPlayers) p.sendMessage(ChatColor.YELLOW + "» Oyuna başlamak için son 30 saniye!");
                } else if (time == 10) {
                    for (Player p : targetPlayers) p.sendMessage(ChatColor.RED + "» Son 10 saniye! Hazırlan!");
                } else if (time <= 5 && time > 0) {
                    for (Player p : targetPlayers) {
                        p.sendTitle(ChatColor.RED + String.valueOf(time), ChatColor.GOLD + "Oyun Başlıyor", 5, 20, 5);
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                } else if (time == 0) {
                    prepBar.removeAll();
                    for (Player p : targetPlayers) {
                        p.sendTitle(ChatColor.GREEN + "BAŞLADI!", ChatColor.YELLOW + "İyi olan kazansın!", 10, 40, 10);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    }
                    
                    // Asıl oyunu başlat
                    LavaGame game = new LavaGame(world, borderSize, center, lavSuresiDk, pvpSuresiDk);
                    game.runTaskTimer(plugin, 0L, 20L); // Saniyede 1 tick güncelle
                    activeGames.put(world.getUID(), game); // Dummy olan veriyi aslıyla değiştirir
                    this.cancel();
                    return;
                }
                
                prepBar.setTitle(ChatColor.YELLOW + "Oyun Hazırlanıyor... Başlamasına: " + ChatColor.RED + time + " s.");
                prepBar.setProgress(Math.max(0.0, (double) time / 60.0));
                
                // Senkronize tut
                for (Player p : targetPlayers) {
                    if (!prepBar.getPlayers().contains(p)) {
                        prepBar.addPlayer(p);
                    }
                }
                
                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) {
            World world = p.getWorld();
            if (activeGames.containsKey(world.getUID())) {
                LavaGame game = activeGames.get(world.getUID());
                if (game != null && (p.getHealth() - event.getFinalDamage() <= 0)) {
                    // Gerçek ölümü engelle
                    event.setCancelled(true);
                    
                    // Sahte ölüm için eşyalarını etrafa saç
                    for (ItemStack item : p.getInventory().getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                            world.dropItemNaturally(p.getLocation(), item);
                        }
                    }
                    p.getInventory().clear();
                    
                    // Üzerindeki zırhları da düşür
                    for (ItemStack armor : p.getInventory().getArmorContents()) {
                        if (armor != null && armor.getType() != Material.AIR) {
                            world.dropItemNaturally(p.getLocation(), armor);
                        }
                    }
                    p.getInventory().setArmorContents(null);
                    
                    // Oyuncunun durumunu sıfırla (Can, yemek, yanma vb.)
                    p.setHealth(20.0);
                    p.setFoodLevel(20);
                    p.setFireTicks(0);
                    p.setFallDistance(0);
                    for (org.bukkit.potion.PotionEffect effect : p.getActivePotionEffects()) {
                        p.removePotionEffect(effect.getType());
                    }
                    
                    // Ölüm sesi
                    world.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
                    
                    // Ölüm ekranı çıkarmadan anında o noktada spectatör yap
                    p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage(ChatColor.RED + "Öldünüz ve elendiniz! Artık oyunu bulunduğunuz noktadan izleyici modunda seyredebilirsiniz.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;
        
        Player p = event.getPlayer();
        World world = p.getWorld();
        
        // Eğer o dünyada oyun var ve henüz hazırlık aşamasındaysa (aktif oyun nesnesi null ise)
        if (activeGames.containsKey(world.getUID()) && activeGames.get(world.getUID()) == null) {
            Location from = event.getFrom();
            // Oyuncunun sadece bakış açısını (pitch/yaw) değiştirmesine izin ver, koordinat hareketini iptal et
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() < to.getY()) {
                Location fixLoc = from.clone();
                fixLoc.setPitch(to.getPitch());
                fixLoc.setYaw(to.getYaw());
                event.setTo(fixLoc);
            }
        }
    }

    @EventHandler
    public void onPlayerPvP(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player pVictim && event.getDamager() instanceof Player pAttacker) {
            World world = pVictim.getWorld();
            if (activeGames.containsKey(world.getUID())) {
                LavaGame game = activeGames.get(world.getUID());
                
                if (game == null) {
                    // game objesi null ise oyun hazırlık aşamasındadır
                    event.setCancelled(true);
                    pAttacker.sendMessage(ChatColor.RED + "Oyun henüz hazırlık aşamasında, PvP yapılamaz!");
                    return;
                }
                
                // Belirtilen PVP süresi henüz dolmadıysa hasarı iptal et
                if (game.timeElapsed < game.pvpStartTime) {
                    event.setCancelled(true);
                    pAttacker.sendMessage(ChatColor.RED + "PvP şu an kapalı! Açılmasına " + formatTime(game.pvpStartTime - game.timeElapsed) + " var.");
                }
            }
        }
    }

    private String formatTime(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        return String.format("%02d:%02d", m, s);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("kur", "baslat", "iptal", "sil");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("baslat")) {
                return Arrays.asList("100", "200", "500", "1000", "2000");
            } else if (args[0].equalsIgnoreCase("kur") || args[0].equalsIgnoreCase("sil")) {
                return Arrays.asList("LavaDunyasi", "Oyun1", "Arena1");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("baslat")) {
                // Lav Suresi (dk)
                return Arrays.asList("10", "20", "30", "40");
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("baslat")) {
                // PvP Suresi (dk)
                return Arrays.asList("5", "10", "15", "20");
            }
        }
        return new ArrayList<>();
    }

    private class LavaGame extends BukkitRunnable {
        private final World world;
        private final int borderSize;
        public final Location center;
        private final BossBar bossBar;
        
        // --- Oyun Ayarları ---
        // (Şablondaki ayarlara uyum sağlandı)
        private final int startingHeight = -64;
        private final int heightIncrease = 1;         // Her yükselişte 1 blok
        private final int heightDelay = 30;           // Kaç saniyede bir yükselecek (30sn)
        private final int gracePeriod;                // Lavlar yükselmeye başlamadan önceki bekleme süresi
        public final int pvpStartTime;                // PvP ne zaman açılacak
        private final Material risingBlock = Material.LAVA;

        public int currentY;
        public int timeElapsed = 0; // Geçen toplam saniye

        public LavaGame(World world, int borderSize, Location center, int lavSuresiDk, int pvpSuresiDk) {
            this.world = world;
            this.borderSize = borderSize;
            this.center = center;
            this.currentY = startingHeight;
            this.gracePeriod = lavSuresiDk * 60;
            this.pvpStartTime = pvpSuresiDk * 60;

            this.bossBar = Bukkit.createBossBar("Floor Is Lava Hazırlanıyor...", BarColor.PURPLE, BarStyle.SOLID);

            // Sınır ve merkez ayarla
            world.getWorldBorder().setCenter(center);
            world.getWorldBorder().setSize(borderSize);
            world.getWorldBorder().setDamageBuffer(0); // Dışarı çıkanı direkt zehirler
        }

        @Override
        public void run() {
            timeElapsed++;

            // BossBar'ı dünyadaki oyuncularla senkronize tut
            updateBossBarPlayers();

            if (timeElapsed <= gracePeriod) {
                // Safhalar: Sadece Gelişme - Lav yükselmeyecek
                int remaining = gracePeriod - timeElapsed;
                bossBar.setTitle(ChatColor.GOLD + "Lavın Başlamasına: " + ChatColor.YELLOW + formatTime(remaining) + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + "Gelişim Sırası");
                bossBar.setProgress(Math.max(0.0, (double) remaining / gracePeriod));
            } else {
                // Lavların yükselme aşaması başladı
                int lavaTime = timeElapsed - gracePeriod;
                
                // Belirlenen delay süresince lavlar yükselir
                if (lavaTime % heightDelay == 0) {
                    fillLavaLayer(currentY);
                    currentY += heightIncrease;
                }

                // PvP Durumu Bilgilendirmesi
                String pvpStatus;
                if (timeElapsed < pvpStartTime) {
                    int pvpLeft = pvpStartTime - timeElapsed;
                    pvpStatus = ChatColor.RED + "PvP'ye Kalan: " + ChatColor.YELLOW + formatTime(pvpLeft);
                    bossBar.setColor(BarColor.RED);
                } else {
                    pvpStatus = ChatColor.DARK_RED + "" + ChatColor.BOLD + "PVP AKTİF!";
                    bossBar.setColor(BarColor.GREEN);
                }
                
                bossBar.setTitle(ChatColor.GOLD + "Lav Seviyesi: " + ChatColor.RED + "Y=" + currentY + ChatColor.DARK_GRAY + " | " + pvpStatus);
                bossBar.setProgress(1.0);
            }

            // Minecraft genelde Max Height 320'dir
            if (currentY > 320) {
                bossBar.setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "OYUN BİTTİ! EKRANINIZ KOMPLE LAV!");
                bossBar.setColor(BarColor.RED);
                this.cancel();
            }
        }

        private void updateBossBarPlayers() {
            List<Player> worldPlayers = world.getPlayers();
            for (Player p : worldPlayers) {
                if (!bossBar.getPlayers().contains(p)) {
                    bossBar.addPlayer(p);
                }
            }
            // Başka dünyaya geçenleri BossBar'dan temizle
            for (Player p : bossBar.getPlayers()) {
                if (!worldPlayers.contains(p)) {
                    bossBar.removePlayer(p);
                }
            }
        }

        private void fillLavaLayer(int y) {
            int radius = borderSize / 2;
            int cx = center.getBlockX();
            int cz = center.getBlockZ();
            
            // OYUN İÇİNDE LAG OLMAMASI ADINA:
            // Sadece Y sınırının altındaki boş bloklar veya sıvıları lav ile değiştirir. 
            // setType(Lava, false) fiziksel blok update'lerini kapatır ve çok hızlı yerleştirir.
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material t = block.getType();
                    if (t.isAir() || t == Material.WATER || t == Material.SEAGRASS || t == Material.TALL_SEAGRASS || t == Material.KELP || t == Material.KELP_PLANT || t == Material.SNOW || t == Material.FERN || t == Material.GRASS || t == Material.TALL_GRASS) {
                        block.setType(risingBlock, false);
                    }
                }
            }
        }

        public void cleanup() {
            bossBar.removeAll();
            world.getWorldBorder().reset();
            this.cancel();
        }
    }
}
