package tr.com.havasaldirisi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FloorIsLavaCommand implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, LavaGame> activeGames = new HashMap<>();

    public FloorIsLavaCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Sadece oyuncular bu komutu kullanabilir.");
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "Bunun için yetkiniz yok.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Kullanım: /lavaisfloor <kur|baslat|iptal|sil> [parametreler]");
            return true;
        }

        World world = player.getWorld();

        if (args[0].equalsIgnoreCase("kur")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Kullanım: /lavaisfloor kur <dünya_adi>");
                return true;
            }
            String worldName = args[1];
            player.sendMessage(ChatColor.GREEN + "Multiverse-Core ile '" + worldName + "' dünyası oluşturuluyor (normal)...");
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mv create " + worldName + " normal");
            player.sendMessage(ChatColor.YELLOW + "Dünya yaratıldığında oraya gitmek için: /mv tp " + worldName);
            return true;

        } else if (args[0].equalsIgnoreCase("baslat")) {
            if (activeGames.containsKey(world.getUID())) {
                player.sendMessage(ChatColor.RED + "Bulunduğunuz dünyada zaten bir oyun çalışıyor!");
                return true;
            }

            int borderSize = 200; // Varsayılan boyut
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

            LavaGame game = new LavaGame(world, borderSize);
            game.runTaskTimer(plugin, 0L, 20L); // Her saniye çalıştır (20 tick)
            activeGames.put(world.getUID(), game);
            
            player.sendMessage(ChatColor.GREEN + "Floor is Lava oyunu " + borderSize + " blok sınırla BAŞLADI!");
            Bukkit.broadcastMessage(ChatColor.RED + "[!] " + ChatColor.LIGHT_PURPLE + world.getName() + ChatColor.GOLD + " dünyasında Zemin Lav oyunu başladı!");
            
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

        player.sendMessage(ChatColor.RED + "Bilinmeyen alt komut. Komutlar: kur, baslat, iptal, sil");
        return true;
    }

    @EventHandler
    public void onPlayerPvP(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player pVictim && event.getDamager() instanceof Player pAttacker) {
            World world = pVictim.getWorld();
            if (activeGames.containsKey(world.getUID())) {
                LavaGame game = activeGames.get(world.getUID());
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
        }
        return new ArrayList<>();
    }

    private class LavaGame extends BukkitRunnable {
        private final World world;
        private final int borderSize;
        private final BossBar bossBar;
        
        // --- Oyun Ayarları ---
        // (Şablondaki ayarlara uyum sağlandı)
        private final int startingHeight = -64;
        private final int heightIncrease = 1;         // Her yükselişte 1 blok
        private final int heightDelay = 30;           // Kaç saniyede bir yükselecek (30sn)
        private final int gracePeriod = 1200;         // 20 dakika boyunca lavlar yükselmez (Saniye = 20 * 60)
        public final int pvpStartTime = 1800;         // 30 dakika sonra PvP serbest (Saniye = 30 * 60)
        private final Material risingBlock = Material.LAVA;

        public int currentY;
        public int timeElapsed = 0; // Geçen toplam saniye

        public LavaGame(World world, int borderSize) {
            this.world = world;
            this.borderSize = borderSize;
            this.currentY = startingHeight;

            this.bossBar = Bukkit.createBossBar("Floor Is Lava Hazırlanıyor...", BarColor.PURPLE, BarStyle.SOLID);

            // Sınır ve merkez ayarla (Bordersize, 0,0 merkezine göre)
            world.getWorldBorder().setCenter(0.0, 0.0);
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
            // OYUN İÇİNDE LAG OLMAMASI ADINA:
            // Sadece Y sınırının altındaki boş bloklar veya sıvıları lav ile değiştirir. 
            // setType(Lava, false) fiziksel blok update'lerini kapatır ve çok hızlı yerleştirir.
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
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
