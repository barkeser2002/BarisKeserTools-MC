package tr.com.havasaldirisi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class FloorIsLavaCommand implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, LavaGame> activeGames = new HashMap<>();
    private final Map<String, Integer> pendingBorders = new HashMap<>();
    
    // --- Etkinlik Toplanma Durumu ---
    private boolean isGathering = false;
    private final Set<UUID> participants = new HashSet<>();
    private BossBar gatheringBar = null;

    public FloorIsLavaCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Sadece oyuncular bu komutu kullanabilir.", NamedTextColor.RED));
            return true;
        }

        if (command.getName().equalsIgnoreCase("etkinligekatil")) {
            if (!isGathering) {
                player.sendMessage(Component.text("Şu anda toplanan veya oyuncu bekleyen bir etkinlik yok!", NamedTextColor.RED));
                return true;
            }
            if (participants.contains(player.getUniqueId())) {
                player.sendMessage(Component.text("Etkinliğe zaten katıldınız, bekleyiniz!", NamedTextColor.YELLOW));
                return true;
            }
            participants.add(player.getUniqueId());
            if (gatheringBar != null) {
                gatheringBar.addPlayer(player);
                gatheringBar.setTitle(toLegacy(
                    Component.text("⚔ Etkinlik Bekleniyor - Katılımcı: ", NamedTextColor.GOLD).append(Component.text(String.valueOf(participants.size()), NamedTextColor.GREEN))
                ));
            }
            player.sendMessage(Component.text("✔ Etkinliğe başarıyla katıldınız! Lütfen oyun başlayana kadar bekleyiniz.", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            
            // Herkese katılım bildirimi
            Bukkit.broadcast(Component.text("» ", NamedTextColor.DARK_GREEN)
                .append(Component.text(player.getName(), NamedTextColor.GREEN))
                .append(Component.text(" etkinliğe katıldı! ", NamedTextColor.YELLOW))
                .append(Component.text("(" + participants.size() + " katılımcı)", NamedTextColor.GRAY)));
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(Component.text("Bunun için yetkiniz yok.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("═══ Floor Is Lava Komutları ═══", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("→ /lavaisfloor kur <dünya> [border]", NamedTextColor.GRAY).append(Component.text(" - Dünya oluştur", NamedTextColor.DARK_GRAY)));
            player.sendMessage(Component.text("→ /lavaisfloor topla", NamedTextColor.GRAY).append(Component.text(" - Katılımı aç", NamedTextColor.DARK_GRAY)));
            player.sendMessage(Component.text("→ /lavaisfloor baslat [border] [lav_dk] [pvp_dk]", NamedTextColor.GRAY).append(Component.text(" - Oyunu başlat", NamedTextColor.DARK_GRAY)));
            player.sendMessage(Component.text("→ /lavaisfloor iptal", NamedTextColor.GRAY).append(Component.text(" - Oyunu iptal et", NamedTextColor.DARK_GRAY)));
            player.sendMessage(Component.text("→ /lavaisfloor sil <dünya>", NamedTextColor.GRAY).append(Component.text(" - Dünyayı sil", NamedTextColor.DARK_GRAY)));
            return true;
        }

        World world = player.getWorld();

        if (args[0].equalsIgnoreCase("kur")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Kullanım: /lavaisfloor kur <dünya_adi> [maxborder]", NamedTextColor.RED));
                return true;
            }
            String worldName = args[1];
            
            if (args.length > 2) {
                try {
                    int border = Integer.parseInt(args[2]);
                    pendingBorders.put(worldName, border);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Border boyutu geçersiz, varsayılan (200) kullanılacak.", NamedTextColor.RED));
                }
            }

            player.sendMessage(Component.text("Multiverse-Core ile '" + worldName + "' dünyası oluşturuluyor. Okyanus olmayan bir bölge aranacak...", NamedTextColor.GREEN));
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
                        
                        boolean badArea = true;
                        while (biomeAttempts < 300 && badArea) {
                            badArea = false;
                            int step = Math.max(16, checkRadius / 4);
                            
                            checkLoop:
                            for (int dx = -checkRadius; dx <= checkRadius; dx += step) {
                                for (int dz = -checkRadius; dz <= checkRadius; dz += step) {
                                    String bName = w.getBiome(loc.getBlockX() + dx, 64, loc.getBlockZ() + dz).name();
                                    if (bName.contains("OCEAN") || bName.contains("RIVER") || bName.contains("SWAMP") || bName.contains("BEACH") || bName.contains("WATER")) {
                                        badArea = true;
                                        break checkLoop;
                                    }
                                }
                            }
                            
                            if (badArea) {
                                loc.add(1000, 0, 1000);
                                biomeAttempts++;
                            }
                        }
                        
                        loc.setY(w.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ()) + 1);
                        w.setSpawnLocation(loc);
                        
                        player.sendMessage(Component.text("► Dünya yaratıldı ve belirlenen oyun sınırı (" + borderSizeForPreload + ") bölgesi denizden ARINDIRILMIŞ kıtaya taşındı!", NamedTextColor.GREEN));
                        player.sendMessage(Component.text("► Yeni Oyun Merkezi: (X:" + loc.getBlockX() + " Z:" + loc.getBlockZ() + ")", NamedTextColor.GREEN));
                        
                        int chunkyRadius = checkRadius + 150;
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "chunky world " + worldName);
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "chunky center " + loc.getBlockX() + " " + loc.getBlockZ());
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "chunky radius " + chunkyRadius);
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "chunky start");
                        player.sendMessage(Component.text("► Chunky ile ön yükleme (Preload) işlemi o bölge için başlatıldı! (Yarıçap: " + chunkyRadius + ")", NamedTextColor.AQUA));
                        
                        player.sendMessage(Component.text("► Oraya gitmek için: /mv tp " + worldName, NamedTextColor.YELLOW));
                        this.cancel();
                    }
                    
                    if (attempts++ > 100) {
                        player.sendMessage(Component.text("Dünya yaratılma süresi çok uzadı, otomatik biyom düzeltmesi pas geçildi.", NamedTextColor.RED));
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 40L, 20L);
            
            return true;

        } else if (args[0].equalsIgnoreCase("topla")) {
            isGathering = true;
            participants.clear();
            if (gatheringBar != null) {
                gatheringBar.removeAll();
            }
            gatheringBar = Bukkit.createBossBar(
                toLegacy(Component.text("⚔ Etkinlik Bekleniyor - Katılımcı: ", NamedTextColor.GOLD).append(Component.text("0", NamedTextColor.GREEN))),
                BarColor.GREEN, BarStyle.SOLID);
            
            // Tüm oyunculara bossbar göster
            for (Player p : Bukkit.getOnlinePlayers()) {
                gatheringBar.addPlayer(p);
            }
            
            Component separator = Component.text("═══════════════════════════════════════════", NamedTextColor.GREEN);
            Bukkit.broadcast(separator);
            Bukkit.broadcast(Component.text("  ⚔ ", NamedTextColor.GOLD).append(Component.text("YENİ BİR FLOOR IS LAVA ETKİNLİĞİ BAŞLIYOR!", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" ⚔", NamedTextColor.GOLD)));
            Bukkit.broadcast(Component.text("  1. Oyuna katılmak için komut: ", NamedTextColor.YELLOW).append(Component.text("/etkinligekatil", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true)));
            Bukkit.broadcast(Component.text("  2. Envanteriniz boş ve hazır olunuz!", NamedTextColor.YELLOW));
            Bukkit.broadcast(separator);
            
            // Ses efekti
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            }
            
            return true;

        } else if (args[0].equalsIgnoreCase("baslat")) {
            if (activeGames.containsKey(world.getUID())) {
                player.sendMessage(Component.text("Bulunduğunuz dünyada zaten bir oyun çalışıyor veya hazırlık aşamasında!", NamedTextColor.RED));
                return true;
            }
            if (participants.isEmpty()) {
                player.sendMessage(Component.text("Hiç kimse katılmadığı için oyunu başlatamazsın! Önce /lavaisfloor topla ile insanları çağır!", NamedTextColor.RED));
                return true;
            }

            int borderSize = pendingBorders.getOrDefault(world.getName(), 200);
            int lavSuresiDk = 20;
            int pvpSuresiDk = 10;
            
            if (args.length > 1) {
                try {
                    borderSize = Integer.parseInt(args[1]);
                    if (borderSize < 100 || borderSize > 5000) {
                        player.sendMessage(Component.text("Border boyutu 100 ile 5000 arasında olmalıdır.", NamedTextColor.RED));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Lütfen geçerli bir sınır sayısı girin.", NamedTextColor.RED));
                    return true;
                }
            }
            if (args.length > 2) {
                try {
                    lavSuresiDk = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Lütfen geçerli bir lav süresi (dakika) girin.", NamedTextColor.RED));
                    return true;
                }
            }
            if (args.length > 3) {
                try {
                    pvpSuresiDk = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Lütfen geçerli bir pvp süresi (dakika) girin.", NamedTextColor.RED));
                    return true;
                }
            }
            
            isGathering = false;
            if (gatheringBar != null) {
                gatheringBar.removeAll();
            }

            activeGames.put(world.getUID(), null);
            startPreparationPhase(player, world, borderSize, lavSuresiDk, pvpSuresiDk);
            
            return true;

        } else if (args[0].equalsIgnoreCase("iptal")) {
            LavaGame game = activeGames.remove(world.getUID());
            if (game != null) {
                game.cleanup();
                player.sendMessage(Component.text("✔ Bu dünyadaki Zemin Lav oyunu başarıyla DURDURULDU.", NamedTextColor.GREEN));
            } else {
                // Hazırlık aşamasındaysa da iptal edebilsin
                if (activeGames.containsKey(world.getUID())) {
                    activeGames.remove(world.getUID());
                    player.sendMessage(Component.text("✔ Hazırlık aşaması iptal edildi.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Bu dünyada devam eden aktif bir zemin lav oyunu bulunmuyor.", NamedTextColor.RED));
                }
            }
            return true;

        } else if (args[0].equalsIgnoreCase("sil")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Kullanım: /lavaisfloor sil <dünya_adi>", NamedTextColor.RED));
                return true;
            }
            String worldName = args[1];
            World targetWorld = Bukkit.getWorld(worldName);
            
            if (targetWorld != null) {
                LavaGame game = activeGames.remove(targetWorld.getUID());
                if (game != null) game.cleanup();

                World defaultWorld = Bukkit.getWorlds().get(0);
                for (Player p : targetWorld.getPlayers()) {
                    p.teleport(defaultWorld.getSpawnLocation());
                    p.sendMessage(Component.text("Bulunduğunuz dünya silindiği için ana dünyaya gönderildiniz.", NamedTextColor.RED));
                }
            }

            player.sendMessage(Component.text("'" + worldName + "' dünyası siliniyor...", NamedTextColor.RED));
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + worldName);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mv confirm");
            return true;
        }

        player.sendMessage(Component.text("Bilinmeyen alt komut. Komutlar: kur, topla, baslat, iptal, sil", NamedTextColor.RED));
        return true;
    }

    private void startPreparationPhase(Player admin, World world, int borderSize, int lavSuresiDk, int pvpSuresiDk) {
        List<Player> targetPlayers = new ArrayList<>();
        List<Player> spectatorPlayers = new ArrayList<>();
        Set<UUID> gamePlayers = new HashSet<>();
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) { // Oyuncuların katılım komutunu kullanan herkesi dâhil et
                if (p.hasPermission("bariskesertools.etkinlik")) {
                    spectatorPlayers.add(p);
                } else {
                    targetPlayers.add(p);
                    gamePlayers.add(p.getUniqueId());
                }
            }
        }
        
        List<Player> allParticipants = new ArrayList<>();
        allParticipants.addAll(targetPlayers);
        allParticipants.addAll(spectatorPlayers);

        int totalPlayers = targetPlayers.size();
        if (totalPlayers == 0) totalPlayers = 1;
        
        Location center = admin.getLocation();
        double radius = Math.max(5.0, totalPlayers / 1.5);
        
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
            tLoc.setY(highestY + 1);
            tLoc.setDirection(center.toVector().subtract(tLoc.toVector()));
            
            p.teleport(tLoc);
            i++;
        }
        for (Player p : spectatorPlayers) {
            Location tLoc = center.clone();
            int highestY = world.getHighestBlockYAt(tLoc.getBlockX(), tLoc.getBlockZ());
            tLoc.setY(highestY + 1);
            p.teleport(tLoc);
            p.setGameMode(GameMode.SPECTATOR);
        }
        
        BossBar prepBar = Bukkit.createBossBar(
            toLegacy(Component.text("⏳ Oyun Hazırlanıyor...", NamedTextColor.YELLOW)),
            BarColor.BLUE, BarStyle.SOLID);
        for (Player p : allParticipants) {
            prepBar.addPlayer(p);
        }

        new BukkitRunnable() {
            int time = 60;
            
            @Override
            public void run() {
                if (!activeGames.containsKey(world.getUID())) {
                    prepBar.removeAll();
                    this.cancel();
                    return;
                }

                if (time == 60) {
                    Component sep = Component.text("═══════════════════════════════════", NamedTextColor.AQUA);
                    for (Player p : allParticipants) {
                        p.sendMessage(sep);
                        p.sendMessage(Component.text("      ⚔ FLOOR IS LAVA BAŞLIYOR! ⚔      ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
                        p.sendMessage(Component.text("1. İlk " + pvpSuresiDk + " dakika maden ve gelişme süresidir (PvP Kapalı).", NamedTextColor.GRAY));
                        p.sendMessage(Component.text("2. Oyunun " + pvpSuresiDk + ". dakikasından itibaren PvP herkes için açılır!", NamedTextColor.GRAY));
                        p.sendMessage(Component.text("3. " + lavSuresiDk + ". dakikanın sonunda lavlar en dipten (-64) yükselmeye başlar.", NamedTextColor.GRAY));
                        p.sendMessage(Component.text("4. Her 5 saniyede bir lav tabakası 1 blok artar.", NamedTextColor.GRAY));
                        p.sendMessage(Component.text("5. Lav Y>100 üstüne çıkınca hızlanır! (3 sn'de 1 blok)", NamedTextColor.GRAY));
                        p.sendMessage(sep);
                    }
                } else if (time == 45) {
                    for (Player p : allParticipants) p.sendMessage(Component.text("» Madene inip eşyalar bulmayı ve yükseklerde üs kurmayı unutmayın!", NamedTextColor.YELLOW));
                } else if (time == 30) {
                    for (Player p : allParticipants) p.sendMessage(Component.text("» Oyuna başlamak için son 30 saniye!", NamedTextColor.YELLOW));
                } else if (time == 10) {
                    for (Player p : allParticipants) p.sendMessage(Component.text("» Son 10 saniye! Hazırlan!", NamedTextColor.RED));
                } else if (time <= 5 && time > 0) {
                    for (Player p : allParticipants) {
                        p.showTitle(Title.title(
                            Component.text(String.valueOf(time), NamedTextColor.RED).decoration(TextDecoration.BOLD, true),
                            Component.text("Oyun Başlıyor", NamedTextColor.GOLD),
                            Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(1), Duration.ofMillis(250))
                        ));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                } else if (time == 0) {
                    prepBar.removeAll();
                    for (Player p : allParticipants) {
                        p.showTitle(Title.title(
                            Component.text("⚔ BAŞLADI! ⚔", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true),
                            Component.text("İyi olan kazansın!", NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
                        ));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    }
                    
                    // ===================================
                    // Eklenti Uyumluluğu Güvenlik Önlemleri
                    // ===================================
                    
                    // 1. WorldGuard (Bölge korumalarını kırabilmeleri için globale build allow ve pvp allow atarız)
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "rg flag __global__ build allow -w " + world.getName());
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "rg flag __global__ pvp allow -w " + world.getName());
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "rg flag __global__ damage-animals allow -w " + world.getName());
                    
                    for (Player p : targetPlayers) {
                        // 2. Essentials (God Mode ve Fly buglarını engelle)
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "god " + p.getName() + " disable");
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "fly " + p.getName() + " disable");
                        
                        // Envanteri temizle ve starter kit ver
                        p.getInventory().clear();
                        p.getInventory().setArmorContents(null);
                        p.setHealth(20.0);
                        p.setFoodLevel(20);
                        p.setSaturation(10.0f);
                        p.setAllowFlight(false);
                        p.setFlying(false);
                        p.setWalkSpeed(0.2f);
                        p.setFlySpeed(0.1f);
                        p.setGameMode(GameMode.SURVIVAL);
                        p.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1));
                        p.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE, 1));
                        p.getInventory().addItem(new ItemStack(Material.STONE_AXE, 1));
                        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                        p.getInventory().addItem(new ItemStack(Material.TORCH, 16));
                        p.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 32));
                    }
                    
                    LavaGame game = new LavaGame(world, borderSize, center, lavSuresiDk, pvpSuresiDk, gamePlayers);
                    game.runTaskTimer(plugin, 0L, 20L);
                    activeGames.put(world.getUID(), game);
                    this.cancel();
                    return;
                }
                
                prepBar.setTitle(toLegacy(
                    Component.text("⏳ Oyun Hazırlanıyor... Başlamasına: ", NamedTextColor.YELLOW).append(Component.text(time + " s.", NamedTextColor.RED))
                ));
                prepBar.setProgress(Math.max(0.0, (double) time / 60.0));
                
                for (Player p : allParticipants) {
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
                
                if (game == null) {
                    event.setCancelled(true);
                    return;
                }
                
                if (p.getGameMode() == GameMode.SPECTATOR) {
                    event.setCancelled(true);
                    return;
                }
                
                if (p.getHealth() - event.getFinalDamage() <= 0) {
                    event.setCancelled(true);
                    
                    for (ItemStack item : p.getInventory().getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                            world.dropItemNaturally(p.getLocation(), item);
                        }
                    }
                    p.getInventory().clear();
                    
                    for (ItemStack armor : p.getInventory().getArmorContents()) {
                        if (armor != null && armor.getType() != Material.AIR) {
                            world.dropItemNaturally(p.getLocation(), armor);
                        }
                    }
                    p.getInventory().setArmorContents(null);
                    
                    p.setHealth(20.0);
                    p.setFoodLevel(20);
                    p.setFireTicks(0);
                    p.setFallDistance(0);
                    for (org.bukkit.potion.PotionEffect effect : p.getActivePotionEffects()) {
                        p.removePotionEffect(effect.getType());
                    }
                    
                    world.playSound(p.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
                    
                    game.eliminatedPlayers.add(p.getUniqueId());
                    
                    World lobby = Bukkit.getWorld("lobby");
                    if (lobby == null && Bukkit.getWorlds().size() > 0) {
                        lobby = Bukkit.getWorlds().get(0);
                    }
                    if (lobby != null) {
                        p.teleport(lobby.getSpawnLocation());
                        p.setGameMode(GameMode.SURVIVAL);
                    } else {
                        p.setGameMode(GameMode.SPECTATOR);
                    }
                    
                    // Ölüm nedenini ve öldüren kişiyi belirle
                    Component deathMessage;
                    EntityDamageEvent.DamageCause cause = event.getCause();
                    Player killer = game.lastDamager.get(p.getUniqueId());
                    
                    if (killer != null && killer.isOnline() && cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                        deathMessage = Component.text(p.getName(), NamedTextColor.YELLOW)
                            .append(Component.text(" oyuncu ", NamedTextColor.RED))
                            .append(Component.text(killer.getName(), NamedTextColor.GOLD))
                            .append(Component.text(" tarafından öldürüldü!", NamedTextColor.RED));
                        game.addKill(killer.getUniqueId());
                    } else if (cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                        if (killer != null && killer.isOnline()) {
                            deathMessage = Component.text(p.getName(), NamedTextColor.YELLOW)
                                .append(Component.text(" lavlara düştü! (Son vuran: ", NamedTextColor.RED))
                                .append(Component.text(killer.getName(), NamedTextColor.GOLD))
                                .append(Component.text(")", NamedTextColor.RED));
                            game.addKill(killer.getUniqueId());
                        } else {
                            deathMessage = Component.text(p.getName(), NamedTextColor.YELLOW)
                                .append(Component.text(" lavlara düşerek elendi!", NamedTextColor.RED));
                        }
                    } else if (cause == EntityDamageEvent.DamageCause.FALL) {
                        deathMessage = Component.text(p.getName(), NamedTextColor.YELLOW)
                            .append(Component.text(" yüksekten düşerek elendi!", NamedTextColor.RED));
                    } else if (cause == EntityDamageEvent.DamageCause.VOID) {
                        deathMessage = Component.text(p.getName(), NamedTextColor.YELLOW)
                            .append(Component.text(" boşluğa düşerek elendi!", NamedTextColor.RED));
                    } else {
                        deathMessage = Component.text(p.getName(), NamedTextColor.YELLOW)
                            .append(Component.text(" elendi!", NamedTextColor.RED));
                    }
                    
                    game.lastDamager.remove(p.getUniqueId());
                    
                    // Elenme title'ı göster
                    p.showTitle(Title.title(
                        Component.text("☠ ELENDİNİZ!", NamedTextColor.RED).decoration(TextDecoration.BOLD, true),
                        Component.text("Oyunu izleyici olarak seyredebilirsiniz.", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
                    ));
                    
                    Component sepLine = Component.text("═══════════════════════════════", NamedTextColor.RED);
                    p.sendMessage(sepLine);
                    p.sendMessage(Component.text("☠ Elendiniz! Oyunu izleyici olarak seyredebilirsiniz.", NamedTextColor.RED));
                    p.sendMessage(Component.text("Kalan oyuncu sayısı: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(game.getRemainingPlayerCount()), NamedTextColor.WHITE)));
                    p.sendMessage(sepLine);
                    
                    for (Player wp : world.getPlayers()) {
                        wp.sendMessage(Component.text("☠ ", NamedTextColor.DARK_RED).append(deathMessage));
                        wp.playSound(wp.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 2.0f);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            World world = victim.getWorld();
            if (activeGames.containsKey(world.getUID())) {
                LavaGame game = activeGames.get(world.getUID());
                if (game != null && !event.isCancelled()) {
                    game.lastDamager.put(victim.getUniqueId(), attacker);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        World world = p.getWorld();
        if (activeGames.containsKey(world.getUID())) {
            LavaGame game = activeGames.get(world.getUID());
            if (game != null && !game.eliminatedPlayers.contains(p.getUniqueId()) && p.getGameMode() != GameMode.SPECTATOR) {
                game.eliminatedPlayers.add(p.getUniqueId());
                Component msg = Component.text("☠ ", NamedTextColor.DARK_RED)
                    .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" oyundan ayrıldığı için elendi!", NamedTextColor.RED));
                for (Player wp : world.getPlayers()) {
                    wp.sendMessage(msg);
                }
            }
        }
        participants.remove(p.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player p = event.getPlayer();
        World world = p.getWorld();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                World currentWorld = p.getWorld();
                if (activeGames.containsKey(currentWorld.getUID())) {
                    LavaGame game = activeGames.get(currentWorld.getUID());
                    if (game != null) {
                        p.setGameMode(GameMode.SPECTATOR);
                        p.sendMessage(Component.text("Etkinlik devam ediyor, elendiğiniz için izleyici modundasınız.", NamedTextColor.YELLOW));
                    }
                }
            }
        }.runTaskLater(plugin, 15L);
        
        handleLobbyItem(p, world);
    }

    @EventHandler
    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        World world = p.getWorld();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                World currentWorld = p.getWorld();
                if (activeGames.containsKey(currentWorld.getUID())) {
                    LavaGame game = activeGames.get(currentWorld.getUID());
                    if (game != null) {
                        p.setGameMode(GameMode.SPECTATOR);
                        p.sendMessage(Component.text("Etkinlik devam ediyor, izleyici modundasınız.", NamedTextColor.YELLOW));
                    }
                }
            }
        }.runTaskLater(plugin, 15L);

        handleLobbyItem(p, world);
    }

    private void handleLobbyItem(Player p, World world) {
        if (!world.getName().equalsIgnoreCase("lobby")) return;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !p.getWorld().getName().equalsIgnoreCase("lobby")) return;

                boolean hasActiveGame = false;
                for (LavaGame game : activeGames.values()) {
                    if (game != null) {
                        hasActiveGame = true;
                        break;
                    }
                }
                
                if (hasActiveGame) {
                    ItemStack spectatorItem = new ItemStack(Material.ENDER_EYE);
                    org.bukkit.inventory.meta.ItemMeta meta = spectatorItem.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text("İzlemeye geç", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
                        meta.lore(java.util.Collections.singletonList(Component.text("Sağ tıklayarak etkinliği izle!", NamedTextColor.GRAY)));
                        spectatorItem.setItemMeta(meta);
                    }
                    
                    boolean hasItem = false;
                    for (ItemStack item : p.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.ENDER_EYE && item.hasItemMeta() && 
                            item.getItemMeta().hasDisplayName() && toLegacy(item.getItemMeta().displayName()).contains("İzlemeye geç")) {
                            hasItem = true;
                            break;
                        }
                    }
                    
                    if (!hasItem) {
                        p.getInventory().setItem(4, spectatorItem);
                    }
                } else {
                    for (int i = 0; i < p.getInventory().getSize(); i++) {
                        ItemStack item = p.getInventory().getItem(i);
                        if (item != null && item.getType() == Material.ENDER_EYE && item.hasItemMeta() && 
                            item.getItemMeta().hasDisplayName() && toLegacy(item.getItemMeta().displayName()).contains("İzlemeye geç")) {
                            p.getInventory().setItem(i, null);
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 10L); // MV-Inventories için 10 tick gecikme
    }

    @EventHandler
    public void onPlayerInteractLobby(org.bukkit.event.player.PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!p.getWorld().getName().equalsIgnoreCase("lobby")) return;
        
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.ENDER_EYE && item.hasItemMeta() && 
            item.getItemMeta().hasDisplayName() && toLegacy(item.getItemMeta().displayName()).contains("İzlemeye geç")) {
            
            event.setCancelled(true);
            
            LavaGame active = null;
            for (LavaGame game : activeGames.values()) {
                if (game != null) {
                    active = game;
                    break;
                }
            }
            
            if (active != null) {
                p.getInventory().remove(item);
                Location specLoc = active.center.clone();
                specLoc.setY(active.world.getHighestBlockYAt(specLoc.getBlockX(), specLoc.getBlockZ()) + 20);
                p.teleport(specLoc);
                p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage(Component.text("Etkinliğe izleyici olarak katıldın!", NamedTextColor.GREEN));
            } else {
                p.sendMessage(Component.text("Şu anda aktif bir etkinlik yok!", NamedTextColor.RED));
                p.getInventory().remove(item);
            }
        }
    }
    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;
        
        Player p = event.getPlayer();
        World world = p.getWorld();
        
        if (activeGames.containsKey(world.getUID()) && activeGames.get(world.getUID()) == null) {
            Location from = event.getFrom();
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
                    event.setCancelled(true);
                    pAttacker.sendMessage(Component.text("Oyun henüz hazırlık aşamasında, PvP yapılamaz!", NamedTextColor.RED));
                    return;
                }
                
                if (game.timeElapsed < game.pvpStartTime) {
                    event.setCancelled(true);
                    pAttacker.sendMessage(Component.text("⚔ PvP şu an kapalı! Açılmasına " + formatTime(game.pvpStartTime - game.timeElapsed) + " var.", NamedTextColor.RED));
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
        if (command.getName().equalsIgnoreCase("etkinligekatil")) {
            return new ArrayList<>();
        }
        if (args.length == 1) {
            List<String> cmds = Arrays.asList("kur", "topla", "baslat", "iptal", "sil");
            List<String> completions = new ArrayList<>();
            for (String c : cmds) {
                if (c.startsWith(args[0].toLowerCase())) completions.add(c);
            }
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("baslat")) {
                return Arrays.asList("100", "200", "500", "1000", "2000");
            } else if (args[0].equalsIgnoreCase("kur") || args[0].equalsIgnoreCase("sil")) {
                List<String> worlds = new ArrayList<>();
                for (World w : Bukkit.getWorlds()) {
                    worlds.add(w.getName());
                }
                worlds.addAll(Arrays.asList("LavaDunyasi", "Oyun1", "Arena1"));
                return worlds;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("baslat")) {
                return Arrays.asList("10", "20", "30", "40");
            } else if (args[0].equalsIgnoreCase("kur")) {
                return Arrays.asList("100", "200", "500", "1000");
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("baslat")) {
                return Arrays.asList("5", "10", "15", "20");
            }
        }
        return new ArrayList<>();
    }

    // ===== Legacy BossBar helper — BossBar API still uses String, not Component =====
    private static String toLegacy(Component component) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
    }
    
    // ===== Firework efekti =====
    private void spawnFireworks(Location loc, int count) {
        for (int i = 0; i < count; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location fwLoc = loc.clone().add(
                        (Math.random() - 0.5) * 6,
                        Math.random() * 2,
                        (Math.random() - 0.5) * 6
                    );
                    Firework fw = loc.getWorld().spawn(fwLoc, Firework.class);
                    FireworkMeta fwMeta = fw.getFireworkMeta();
                    fwMeta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.YELLOW, Color.ORANGE, Color.RED)
                        .withFade(Color.WHITE)
                        .flicker(true)
                        .trail(true)
                        .build());
                    fwMeta.setPower(1);
                    fw.setFireworkMeta(fwMeta);
                }
            }.runTaskLater(plugin, i * 10L);
        }
    }

    private class LavaGame extends BukkitRunnable {
        private final World world;
        private final int borderSize;
        public final Location center;
        private final BossBar bossBar;
        
        private final int startingHeight = -64;
        private final int heightIncrease = 1;
        private final int gracePeriod;
        public final int pvpStartTime;
        private final Material risingBlock = Material.LAVA;

        public int currentY;
        public int timeElapsed = 0;
        
        public final Set<UUID> activePlayers = new HashSet<>();
        public final Set<UUID> eliminatedPlayers = new HashSet<>();
        public final Map<UUID, Integer> killCounts = new HashMap<>();
        public final Map<UUID, Player> lastDamager = new HashMap<>();
        private boolean gameEnded = false;
        private boolean pvpAnnounced = false;

        public LavaGame(World world, int borderSize, Location center, int lavSuresiDk, int pvpSuresiDk, Set<UUID> initialPlayers) {
            this.world = world;
            this.borderSize = borderSize;
            this.center = center;
            this.currentY = startingHeight;
            this.gracePeriod = lavSuresiDk * 60;
            this.pvpStartTime = pvpSuresiDk * 60;
            this.activePlayers.addAll(initialPlayers);

            this.bossBar = Bukkit.createBossBar("Floor Is Lava Hazırlanıyor...", BarColor.PURPLE, BarStyle.SOLID);

            world.getWorldBorder().setCenter(center);
            world.getWorldBorder().setSize(borderSize);
            world.getWorldBorder().setDamageBuffer(0);
        }
        
        public void addKill(UUID killerUUID) {
            killCounts.put(killerUUID, killCounts.getOrDefault(killerUUID, 0) + 1);
        }
        
        public int getRemainingPlayerCount() {
            int count = 0;
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() != GameMode.SPECTATOR && activePlayers.contains(p.getUniqueId()) && !eliminatedPlayers.contains(p.getUniqueId())) {
                    count++;
                }
            }
            return count;
        }
        
        // Dinamik lav hızı: Y>100 olunca 3 saniyede 1 blok, normalde 5 saniyede 1 blok
        private int getCurrentHeightDelay() {
            if (currentY > 100) return 3;
            return 5;
        }

        @Override
        public void run() {
            if (gameEnded) return;
            timeElapsed++;

            updateBossBarPlayers();
            
            int remainingPlayers = getRemainingPlayerCount();
            
            // PvP açılma duyurusu (bir kez)
            if (!pvpAnnounced && timeElapsed >= pvpStartTime) {
                pvpAnnounced = true;
                for (Player p : world.getPlayers()) {
                    p.showTitle(Title.title(
                        Component.text("⚔ PVP AKTİF! ⚔", NamedTextColor.RED).decoration(TextDecoration.BOLD, true),
                        Component.text("Artık diğer oyuncularla savaşabilirsiniz!", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
                    ));
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
                }
                broadcastToWorld(Component.text("⚔ ", NamedTextColor.RED)
                    .append(Component.text("PVP AKTİF!", NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
                    .append(Component.text(" Artık birbirinizle savaşabilirsiniz!", NamedTextColor.YELLOW)));
            }
            
            Component pvpStatus;
            if (timeElapsed < pvpStartTime) {
                int pvpLeft = pvpStartTime - timeElapsed;
                pvpStatus = Component.text("PvP: ", NamedTextColor.RED).append(Component.text(formatTime(pvpLeft), NamedTextColor.YELLOW));
            } else {
                pvpStatus = Component.text("PVP AKTİF!", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true);
            }

            if (timeElapsed <= gracePeriod) {
                int remaining = gracePeriod - timeElapsed;
                Component title = Component.text("Kalan: ", NamedTextColor.AQUA)
                    .append(Component.text(String.valueOf(remainingPlayers), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Lav: ", NamedTextColor.GOLD))
                    .append(Component.text(formatTime(remaining), NamedTextColor.YELLOW))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(pvpStatus);
                bossBar.setTitle(toLegacy(title));
                bossBar.setProgress(Math.max(0.0, (double) remaining / gracePeriod));
                
                if (timeElapsed >= pvpStartTime) {
                    bossBar.setColor(BarColor.GREEN);
                } else {
                    bossBar.setColor(BarColor.PURPLE);
                }
                
                // Lav uyarı mesajları
                if (remaining == 60) {
                    broadcastToWorld(Component.text("⚠ Lavlar 1 dakika sonra yükselmeye başlayacak! Yüksek yerlere çıkın!", NamedTextColor.GOLD));
                    for (Player p : world.getPlayers()) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                } else if (remaining == 30) {
                    broadcastToWorld(Component.text("⚠ Lavlar 30 saniye sonra yükselmeye başlıyor!", NamedTextColor.RED));
                    for (Player p : world.getPlayers()) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.8f);
                } else if (remaining == 10) {
                    broadcastToWorld(Component.text("⚠ ", NamedTextColor.DARK_RED).append(Component.text("LAVLAR 10 SANİYE SONRA YÜKSELİYOR!", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true)));
                    for (Player p : world.getPlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                }
            } else {
                int lavaTime = timeElapsed - gracePeriod;
                int currentDelay = getCurrentHeightDelay();
                
                if (lavaTime % currentDelay == 0) {
                    fillLavaLayer(currentY);
                    currentY += heightIncrease;
                    
                    for (Player p : world.getPlayers()) {
                        if (p.getGameMode() != GameMode.SPECTATOR && p.getLocation().getY() - currentY < 10) {
                            p.sendMessage(Component.text("⚠ Lav seviyesi size yaklaşıyor! (Y=" + currentY + ")", NamedTextColor.RED));
                            p.playSound(p.getLocation(), Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
                            // Action bar uyarısı
                            p.sendActionBar(Component.text("⚠ LAV: Y=" + currentY + " ⚠", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
                        }
                    }
                    
                    // Hızlanma uyarısı (bir kez)
                    if (currentY == 101) {
                        broadcastToWorld(Component.text("⚡ ", NamedTextColor.RED)
                            .append(Component.text("LAVLAR HIZLANIYOR!", NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
                            .append(Component.text(" Artık her 3 saniyede bir yükseliyor!", NamedTextColor.YELLOW)));
                        for (Player p : world.getPlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.5f);
                    }
                }
                
                if (lavaTime % 60 == 0 && lavaTime > 0) {
                    double currentBorder = world.getWorldBorder().getSize();
                    double newSize = Math.max(50, currentBorder * 0.95);
                    world.getWorldBorder().setSize(newSize, 10);
                    broadcastToWorld(Component.text("⚠ Sınır daralıyor! Yeni sınır: ", NamedTextColor.GOLD).append(Component.text((int) newSize + " blok", NamedTextColor.RED)));
                }

                Component lavaTitle = Component.text("Kalan: ", NamedTextColor.AQUA)
                    .append(Component.text(String.valueOf(remainingPlayers), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Lav: ", NamedTextColor.GOLD))
                    .append(Component.text("Y=" + currentY, NamedTextColor.RED))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(pvpStatus);
                bossBar.setTitle(toLegacy(lavaTitle));
                bossBar.setColor(timeElapsed >= pvpStartTime ? BarColor.RED : BarColor.GREEN);
                bossBar.setProgress(1.0);
            }

            // Oyun bitiş kontrolü
            if (currentY > 320 || remainingPlayers <= 1) {
                gameEnded = true;
                
                if (remainingPlayers == 1) {
                    Player winner = null;
                    for (Player p : world.getPlayers()) {
                        if (p.getGameMode() != GameMode.SPECTATOR && activePlayers.contains(p.getUniqueId()) && !eliminatedPlayers.contains(p.getUniqueId())) {
                            winner = p;
                            break;
                        }
                    }
                    
                    if (winner != null) {
                        String winnerName = winner.getName();
                        int winnerKills = killCounts.getOrDefault(winner.getUniqueId(), 0);
                        
                        bossBar.setTitle(toLegacy(
                            Component.text("★ ", NamedTextColor.GOLD)
                                .append(Component.text("KAZANAN: " + winnerName, NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true))
                                .append(Component.text(" ★", NamedTextColor.GOLD))
                        ));
                        bossBar.setColor(BarColor.YELLOW);
                        
                        Component sep = Component.text("═══════════════════════════════════════", NamedTextColor.GREEN);
                        for (Player p : world.getPlayers()) {
                            p.sendMessage(sep);
                            p.sendMessage(Component.text("  ★ ", NamedTextColor.GOLD).append(Component.text("OYUN BİTTİ!", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)).append(Component.text(" ★", NamedTextColor.GOLD)));
                            p.sendMessage(Component.text("  Kazanan: ", NamedTextColor.YELLOW).append(Component.text(winnerName, NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)));
                            p.sendMessage(Component.text("  Kill Sayısı: ", NamedTextColor.YELLOW).append(Component.text(String.valueOf(winnerKills), NamedTextColor.WHITE)));
                            p.sendMessage(Component.text("  Oyun Süresi: ", NamedTextColor.YELLOW).append(Component.text(formatTime(timeElapsed), NamedTextColor.WHITE)));
                            p.sendMessage(sep);
                            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        }
                        
                        winner.showTitle(Title.title(
                            Component.text("★ TEBRİKLER! ★", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true),
                            Component.text("Floor Is Lava kazananısınız!", NamedTextColor.GREEN),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
                        ));
                        
                        // Kazanana firework efekti
                        spawnFireworks(winner.getLocation(), 5);
                    }
                } else {
                    bossBar.setTitle(toLegacy(Component.text("OYUN BİTTİ! (Kimse kalmadı)", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true)));
                    bossBar.setColor(BarColor.BLUE);
                }
                
                showKillLeaderboard();
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Player p : world.getPlayers()) {
                            if (p.getGameMode() == GameMode.SPECTATOR && activePlayers.contains(p.getUniqueId())) {
                                p.setGameMode(GameMode.SURVIVAL);
                            }
                        }
                        cleanup();
                        activeGames.remove(world.getUID());
                    }
                }.runTaskLater(plugin, 200L);
                
                this.cancel();
            }
        }
        
        private void broadcastToWorld(Component message) {
            for (Player p : world.getPlayers()) {
                p.sendMessage(message);
            }
        }
        
        private void showKillLeaderboard() {
            if (killCounts.isEmpty()) return;
            
            List<Map.Entry<UUID, Integer>> sorted = killCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());
            
            for (Player p : world.getPlayers()) {
                p.sendMessage(Component.text("  ── Kill Sıralaması ──", NamedTextColor.GOLD));
                int rank = 1;
                String[] medals = {"🥇", "🥈", "🥉"};
                for (Map.Entry<UUID, Integer> entry : sorted) {
                    Player killer = Bukkit.getPlayer(entry.getKey());
                    String name = killer != null ? killer.getName() : "Bilinmiyor";
                    p.sendMessage(Component.text("  " + medals[rank - 1] + " ", NamedTextColor.YELLOW)
                        .append(Component.text(name, NamedTextColor.WHITE))
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(entry.getValue() + " kill", NamedTextColor.RED)));
                    rank++;
                }
            }
        }

        private void updateBossBarPlayers() {
            List<Player> worldPlayers = world.getPlayers();
            for (Player p : worldPlayers) {
                if (!bossBar.getPlayers().contains(p)) {
                    bossBar.addPlayer(p);
                }
            }
            for (Player p : new ArrayList<>(bossBar.getPlayers())) {
                if (!worldPlayers.contains(p)) {
                    bossBar.removePlayer(p);
                }
            }
        }

        private void fillLavaLayer(int y) {
            int radius = borderSize / 2;
            int cx = center.getBlockX();
            int cz = center.getBlockZ();
            
            new BukkitRunnable() {
                int x = cx - radius;
                int z = cz - radius;
                final int maxPerTick = Math.max(2500, (borderSize * borderSize) / 40);

                @Override
                public void run() {
                    if (gameEnded) {
                        this.cancel();
                        return;
                    }
                    int count = 0;
                    while (x <= cx + radius) {
                        while (z <= cz + radius) {
                            Block block = world.getBlockAt(x, y, z);
                            Material t = block.getType();
                            if (t.isAir() || t == Material.WATER || t == Material.SEAGRASS || t == Material.TALL_SEAGRASS || t == Material.KELP || t == Material.KELP_PLANT || t == Material.SNOW || t == Material.FERN || t == Material.GRASS || t == Material.TALL_GRASS) {
                                block.setType(risingBlock, false);
                            }
                            z++;
                            count++;
                            if (count >= maxPerTick) {
                                return; // Yükü diğer ticklere böl
                            }
                        }
                        z = cz - radius;
                        x++;
                    }
                    this.cancel();
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        public void cleanup() {
            bossBar.removeAll();
            world.getWorldBorder().reset();
            
            // Bütün oyuncuları lobby dünyasına ışınla ve sıfırla
            World lobby = Bukkit.getWorld("lobby");
            if (lobby == null && Bukkit.getWorlds().size() > 0) {
                lobby = Bukkit.getWorlds().get(0);
            }
            if (lobby != null) {
                for (Player p : world.getPlayers()) {
                    p.teleport(lobby.getSpawnLocation());
                    p.setGameMode(GameMode.SURVIVAL);
                    p.getInventory().clear();
                    p.getInventory().setArmorContents(null);
                    p.setHealth(20.0);
                    p.setFoodLevel(20);
                    p.setFireTicks(0);
                    for (org.bukkit.potion.PotionEffect effect : p.getActivePotionEffects()) {
                        p.removePotionEffect(effect.getType());
                    }
                    p.sendMessage(Component.text("Etkinlik bitti, lobiye gönderildiniz.", NamedTextColor.YELLOW));
                }
            }
            
            // Dünyayı sil
            String wName = world.getName();
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + wName);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mv confirm");
            
            try { this.cancel(); } catch (IllegalStateException ignored) {}
        }
    }
}
