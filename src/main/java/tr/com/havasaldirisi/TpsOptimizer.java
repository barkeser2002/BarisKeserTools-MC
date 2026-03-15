package tr.com.havasaldirisi;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;

public class TpsOptimizer implements Listener {

    private final JavaPlugin plugin;

    public TpsOptimizer(JavaPlugin plugin) {
        this.plugin = plugin;
        startPlayerRenderOptimizer();
    }

    /**
     * 1) Blokların kırılarak yere 100'lerce Item ve XP Orb Entity'si saçıp 
     * CPU'yu (TPS'i) felç etmesini engeller. Droplar DİREKT oyuncuya gider.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        
        // Sadece hayatta kalma modundakiler için geçerli
        if (p.getGameMode() == GameMode.CREATIVE) return;

        Block block = event.getBlock();
        
        // Oyuncunun elindeki alete (verimlilik/ipeksi dokunuş) göre düşecekleri hesaplar
        Collection<ItemStack> drops = block.getDrops(p.getInventory().getItemInMainHand());

        if (!drops.isEmpty()) {
            event.setDropItems(false); // Blokların yere eşya atma motorunu KOMPLE kapatır

            for (ItemStack drop : drops) {
                // Direk oyuncunun envanterine eklemeyi dener
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(drop);
                
                // Sadece eğer oyuncunun üstü FULL ise yere düşer (yüksek oranda entity drop engellenir)
                if (!leftover.isEmpty()) {
                    for (ItemStack left : leftover.values()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), left);
                    }
                }
            }
        }

        // Exp Orb'ları (yeşil toplar) inanılmaz render lagı sokar, bunu da direkt olarak oyuncuya yükleriz.
        int exp = event.getExpToDrop();
        if (exp > 0) {
            event.setExpToDrop(0); // Yere düşmesini engeller
            p.giveExp(exp); // Fiziksel hasaplamalara girmeden hesaba ekler
            // Minik bir ses vererek tatmin hissi yaratır. (Gerçekçi pitch efekti verebilirsiniz)
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, (float)(1.5 + Math.random() * 0.5));
        }
    }

    /**
     * 2) 100 blokluk (1 Chunk=16x16) çok dar bir alana eğer 50 oyuncu birden girerse, 
     * Sunucu her biri için diğerlerini render koduna (raytrace packetlerine) sokar, FPS'yi ve TPS'i felç eder. 
     * Buna Çözüm: Görüş kısıtlama optimizasyonu!
     */
    private void startPlayerRenderOptimizer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p1 : Bukkit.getOnlinePlayers()) {
                    
                    // Sadece kalabalık dünyalar içindir (örn: > 25 kişi olduğunda optimize et, az kişide gereksiz işlem yapma)
                    if (p1.getWorld().getPlayers().size() < 25) {
                        for (Player p2 : p1.getWorld().getPlayers()) {
                            if (p1 != p2 && !p1.canSee(p2)) {
                                p1.showPlayer(plugin, p2);
                            }
                        }
                        continue;
                    }

                    // Kalabalık ortam! Çok uzakları sunucunun NMS rendering motorundan gizle ve rahatlat.
                    for (Player p2 : p1.getWorld().getPlayers()) {
                        if (p1 == p2) continue;

                        double distSq = p1.getLocation().distanceSquared(p2.getLocation());
                        
                        // Histerezis Mantığı (titremeleri engellemek için iki sınır koyulur)
                        if (distSq > (55.0 * 55.0)) { // Mesafe > 55 Blok ise tamamen Gizle (Network paketi kesilir = Mükemmel TPS artışı!)
                            if (p1.canSee(p2)) {
                                p1.hidePlayer(plugin, p2);
                            }
                        } else if (distSq < (45.0 * 45.0)) { // 45 Bloğa yaklaştıklarında tekrar ortaya çıkar (PvP mesafesi güvende kalır)
                            if (!p1.canSee(p2)) {
                                p1.showPlayer(plugin, p2);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 40L); // Oyunu hiç yormamak için her 2 Saniyede bir arka planda kontrol eder
    }
}
